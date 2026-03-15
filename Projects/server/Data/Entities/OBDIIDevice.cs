namespace server.Models.Entities
{
    public class OBDIIDevice
    {
        public uint DeviceId { get; set; }
        public DateTime CreatedAt { get; set; }
        public string MacAddress { get; set; } = null!;
    }
}
