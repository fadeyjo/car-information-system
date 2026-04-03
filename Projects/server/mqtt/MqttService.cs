using Microsoft.Extensions.Options;
using MQTTnet;
using server.Contracts.Requests;
using server.Models.Entities;
using server.Services.Interfaces;
using System.Text;
using System.Text.Json;

namespace server.mqtt
{
    public class MqttService
    {
        private readonly IMqttClient _client;
        private readonly MqttClientOptions _options;
        private readonly IServiceScopeFactory _scopeFactory;

        public const string NEW_TELEMETRY_DATA_TOPIC = "telemetry/new-data";
        public const string NEW_GPS_DATA_TOPIC = "gps/new-data";

        public MqttService(IServiceScopeFactory scopeFactory, IOptions<MqttSettings> mqttOptions)
        {
            _scopeFactory = scopeFactory;

            var settings = mqttOptions.Value;

            var factory = new MqttClientFactory();
            _client = factory.CreateMqttClient();

            _options = new MqttClientOptionsBuilder()
                .WithTcpServer(settings.Host, settings.Port)
                .WithClientId($"aspnet-client-{Guid.NewGuid()}")
                .WithCleanSession()
                .Build();

            _client.ApplicationMessageReceivedAsync += async e =>
            {
                var topic = e.ApplicationMessage.Topic;
                var payload = Encoding.UTF8.GetString(e.ApplicationMessage.Payload);

                using var scope = _scopeFactory.CreateScope();

                var telemetryService = scope.ServiceProvider
                    .GetRequiredService<ITelemetryDataService>();

                var gpsService = scope.ServiceProvider
                    .GetRequiredService<IGpsDataService>();

                switch (topic)
                {
                    case NEW_TELEMETRY_DATA_TOPIC:
                        await CreateTelemetryData(payload, telemetryService);
                        break;

                    case NEW_GPS_DATA_TOPIC:
                        await CreateGpsData(payload, gpsService);
                        break;
                }
            };
        }

        private async Task CreateTelemetryData(string payload, ITelemetryDataService telemetryService)
        {
            try
            {
                var data = JsonSerializer.Deserialize<CreateTelemetryDataRequest>(
                    payload,
                    new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    }
                );

                if (data == null)
                {
                    return;
                }

                await telemetryService.CreateTelemetryData(
                    data.RecDatetime, data.ServiceId, data.PID,
                    data.EcuId, data.ResponseDlc, data.Response,
                    data.TripId
                );
            }
            catch (Exception ex)
            {

            }
        }

        private async Task CreateGpsData(string payload, IGpsDataService gpsService)
        {
            var data = JsonSerializer.Deserialize<CreateGpsDataRequest>(payload);

            if (data == null)
            {
                return;
            }

            await gpsService.CreateGpsData(
                data.RecDateTime, data.TripId, data.LatitudeDeg,
                data.LongitudeDeg, data.AccuracyM, data.SpeedKmh,
                data.BearingDeg
            );
        }

        public async Task ConnectAsync()
        {
            if (!_client.IsConnected)
            {
                await _client.ConnectAsync(_options);
            }
        }

        public async Task SubscribeAsync(string topic)
        {
            await _client.SubscribeAsync(topic);
        }

        public async Task PublishAsync(string topic, string message)
        {
            var mqttMessage = new MqttApplicationMessageBuilder()
                .WithTopic(topic)
                .WithPayload(message)
                .Build();

            await _client.PublishAsync(mqttMessage);
        }
    }
}
