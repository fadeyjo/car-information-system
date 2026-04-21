using Microsoft.Extensions.Options;
using MQTTnet;
using MQTTnet.Protocol;
using server.Contracts.Requests;
using server.Services.Interfaces;
using System.Buffers;
using System.Text;
using System.Text.Json;
using System.Threading.Channels;

namespace server.mqtt;

public sealed class MqttIngestionHostedService : BackgroundService
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly MqttSettings _settings;
    private readonly ILogger<MqttIngestionHostedService> _logger;

    private IMqttClient? _client;
    private Channel<MqttInboundMessage>? _channel;

    public const string NEW_TELEMETRY_DATA_TOPIC = "telemetry/new-data";
    public const string NEW_GPS_DATA_TOPIC = "gps/new-data";

    public MqttIngestionHostedService(
        IServiceScopeFactory scopeFactory,
        IOptions<MqttSettings> mqttOptions,
        ILogger<MqttIngestionHostedService> logger)
    {
        _scopeFactory = scopeFactory;
        _settings = mqttOptions.Value;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _channel = Channel.CreateBounded<MqttInboundMessage>(
            new BoundedChannelOptions(_settings.InboundQueueCapacity)
            {
                SingleReader = false,
                SingleWriter = true,
                FullMode = BoundedChannelFullMode.DropOldest
            });

        using var parallelism = new SemaphoreSlim(_settings.InboundMaxParallelism, _settings.InboundMaxParallelism);

        var workers = Enumerable.Range(0, _settings.InboundMaxParallelism)
            .Select(i => Task.Run(() => WorkerLoop(i, _channel.Reader, parallelism, stoppingToken), stoppingToken))
            .ToArray();

        await RunMqttLoopAsync(_channel.Writer, stoppingToken);

        _channel.Writer.TryComplete();
        await Task.WhenAll(workers);
    }

    private async Task RunMqttLoopAsync(ChannelWriter<MqttInboundMessage> writer, CancellationToken stoppingToken)
    {
        var factory = new MqttClientFactory();
        _client = factory.CreateMqttClient();

        _client.ApplicationMessageReceivedAsync += e =>
        {
            try
            {
                var topic = e.ApplicationMessage.Topic;
                var payloadBytes = e.ApplicationMessage.Payload.ToArray();
                var payload = Encoding.UTF8.GetString(payloadBytes);

                if (!writer.TryWrite(new MqttInboundMessage(topic, payload, DateTimeOffset.UtcNow)))
                {
                    _logger.LogWarning("MQTT inbound queue is closed; dropping message for topic {Topic}", topic);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to enqueue MQTT message");
            }

            return Task.CompletedTask;
        };

        var options = new MqttClientOptionsBuilder()
            .WithTcpServer(_settings.Host, _settings.Port)
            .WithClientId($"aspnet-ingest-{Guid.NewGuid()}")
            .WithCleanSession()
            .Build();

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                if (!_client.IsConnected)
                {
                    _logger.LogInformation("Connecting to MQTT broker {Host}:{Port}", _settings.Host, _settings.Port);
                    await _client.ConnectAsync(options, stoppingToken);
                    _logger.LogInformation("Connected to MQTT broker");

                    await _client.SubscribeAsync(
                        new MqttTopicFilterBuilder().WithTopic(NEW_TELEMETRY_DATA_TOPIC).WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce).Build(),
                        stoppingToken);

                    await _client.SubscribeAsync(
                        new MqttTopicFilterBuilder().WithTopic(NEW_GPS_DATA_TOPIC).WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce).Build(),
                        stoppingToken);

                    _logger.LogInformation("Subscribed to MQTT topics");
                }

                await Task.Delay(TimeSpan.FromSeconds(2), stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "MQTT loop error; retrying soon");
                await Task.Delay(TimeSpan.FromSeconds(3), stoppingToken);
            }
        }

        try
        {
            if (_client.IsConnected)
            {
                await _client.DisconnectAsync(cancellationToken: CancellationToken.None);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to disconnect MQTT client cleanly");
        }
    }

    private async Task WorkerLoop(
        int workerId,
        ChannelReader<MqttInboundMessage> reader,
        SemaphoreSlim parallelism,
        CancellationToken stoppingToken)
    {
        await foreach (var msg in reader.ReadAllAsync(stoppingToken))
        {
            await parallelism.WaitAsync(stoppingToken);
            try
            {
                await ProcessMessageAsync(msg, stoppingToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Worker {WorkerId} failed processing topic {Topic}", workerId, msg.Topic);
            }
            finally
            {
                parallelism.Release();
            }
        }
    }

    private async Task ProcessMessageAsync(MqttInboundMessage msg, CancellationToken ct)
    {
        using var scope = _scopeFactory.CreateScope();

        switch (msg.Topic)
        {
            case NEW_TELEMETRY_DATA_TOPIC:
                {
                    var telemetryService = scope.ServiceProvider.GetRequiredService<ITelemetryDataService>();
                    var data = Deserialize<CreateTelemetryDataRequest>(msg.Payload);
                    if (data == null) return;

                    await telemetryService.CreateTelemetryData(
                        data.RecDatetime, data.ServiceId, data.PID,
                        data.EcuId, data.ResponseDlc, data.Response,
                        data.TripId
                    );

                    await TryPublishDerivedTelemetryAsync(data, ct);
                    break;
                }

            case NEW_GPS_DATA_TOPIC:
                {
                    var gpsService = scope.ServiceProvider.GetRequiredService<IGpsDataService>();
                    var data = Deserialize<CreateGpsDataRequest>(msg.Payload);
                    if (data == null) return;

                    await gpsService.CreateGpsData(
                        data.RecDateTime, data.TripId, data.LatitudeDeg,
                        data.LongitudeDeg, data.AccuracyM, data.SpeedKmh,
                        data.BearingDeg
                    );
                    break;
                }

            default:
                break;
        }
    }

    private async Task TryPublishDerivedTelemetryAsync(CreateTelemetryDataRequest data, CancellationToken ct)
    {
        if (_client is null || !_client.IsConnected) return;
        if (data.Response is null || data.Response.Length < 5) return;

        var pid = data.Response[2];

        string? topic = null;
        string? payload = null;

        switch (pid)
        {
            case 0x0C:
                {
                    var a = data.Response[3];
                    var b = data.Response[4];
                    var rpm = (((a * 256) + b) / 4);
                    topic = $"telemetry/RPM/{data.TripId}";
                    payload = rpm.ToString();
                    break;
                }
            case 0x0D:
                {
                    var a = data.Response[3];
                    var b = data.Response[4];
                    var speed = a;
                    topic = $"telemetry/speed/{data.TripId}";
                    payload = speed.ToString();
                    break;
                }
            case 0x05:
                {
                    var a = data.Response[3];
                    var b = data.Response[4];
                    var temperature = a - 40;
                    topic = $"telemetry/tempreture/{data.TripId}";
                    payload = temperature.ToString();
                    break;
                }
            default:
                return;
        }

        var message = new MqttApplicationMessageBuilder()
            .WithTopic(topic)
            .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce)
            .WithPayload(payload)
            .Build();

        await _client.PublishAsync(message, ct);
    }

    private T? Deserialize<T>(string payload)
    {
        try
        {
            return JsonSerializer.Deserialize<T>(
                payload,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true }
            );
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to deserialize MQTT payload to {Type}", typeof(T).Name);
            return default;
        }
    }

    private readonly record struct MqttInboundMessage(string Topic, string Payload, DateTimeOffset ReceivedAt);
}

