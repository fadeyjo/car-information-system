namespace server.mqtt
{
    public class MqttSettings
    {
        public string Host { get; set; } = null!;
        public int Port { get; set; }

        public int InboundQueueCapacity { get; set; } = 50000;
        public int InboundMaxParallelism { get; set; } = Math.Max(1, Environment.ProcessorCount / 2);
    }
}
