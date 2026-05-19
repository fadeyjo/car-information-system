using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using MQTTnet;
using MQTTnet.Protocol;

namespace server.mqtt;
public sealed class MqttOutboundPublisher : IMqttOutboundPublisher, IDisposable
{
    private readonly MqttSettings _settings;
    private readonly ILogger<MqttOutboundPublisher> _logger;
    private readonly SemaphoreSlim _connectLock = new(1, 1);
    private IMqttClient? _client;
    private bool _disposed;

    public MqttOutboundPublisher(IOptions<MqttSettings> mqttOptions, ILogger<MqttOutboundPublisher> logger)
    {
        _settings = mqttOptions.Value;
        _logger = logger;
    }

    public async Task PublishEmptyAsync(string topic, CancellationToken cancellationToken = default)
    {
        ObjectDisposedException.ThrowIf(_disposed, this);

        await EnsureConnectedAsync(cancellationToken).ConfigureAwait(false);

        var message = new MqttApplicationMessageBuilder()
            .WithTopic(topic)
            .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtLeastOnce)
            .WithPayload(Array.Empty<byte>())
            .Build();

        await _client!.PublishAsync(message, cancellationToken).ConfigureAwait(false);
    }

    private async Task EnsureConnectedAsync(CancellationToken cancellationToken)
    {
        if (_client?.IsConnected == true)
        {
            return;
        }

        await _connectLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            if (_client?.IsConnected == true)
            {
                return;
            }

            if (_client is not null)
            {
                await _client.DisconnectAsync().ConfigureAwait(false);
                _client.Dispose();
                _client = null;
            }

            var factory = new MqttClientFactory();
            _client = factory.CreateMqttClient();

            var options = new MqttClientOptionsBuilder()
                .WithTcpServer(_settings.Host, _settings.Port)
                .WithClientId($"aspnet-publish-{Guid.NewGuid()}")
                .WithCleanSession()
                .Build();

            _logger.LogInformation("MQTT outbound connecting to {Host}:{Port}", _settings.Host, _settings.Port);
            await _client.ConnectAsync(options, cancellationToken).ConfigureAwait(false);
            _logger.LogInformation("MQTT outbound connected");
        }
        finally
        {
            _connectLock.Release();
        }
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        _connectLock.Dispose();
        _client?.Dispose();
        _client = null;
    }
}
