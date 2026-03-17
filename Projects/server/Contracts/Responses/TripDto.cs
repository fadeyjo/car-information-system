namespace server.Contracts.Responses
{
    public class TripDto
    {
        public ulong TripId { get; set; }
        public DateTime StartDatetime { get; set; }
        public uint DeviceId { get; set; }
        public uint CarId { get; set; }
        public DateTime? EndDatetime { get; set; }
    }
}
