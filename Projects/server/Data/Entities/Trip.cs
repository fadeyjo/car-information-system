namespace server.Models.Entities
{
    public class Trip
    {
        public ulong TripId { get; set; }
        public DateTime StartDatetime { get; set; }
        public uint DeviceId { get; set; }
        public uint CarId { get; set; }
        public DateTime? EndDatetime { get; set; }
        public OBDIIDevice OBDIIDevice { get; set; } = null!;
        public Car Car { get; set; } = null!;
    }
}
