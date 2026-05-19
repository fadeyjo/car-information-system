namespace server.mqtt;

public interface IMqttOutboundPublisher
{
    Task PublishEmptyAsync(string topic, CancellationToken cancellationToken = default);
}
