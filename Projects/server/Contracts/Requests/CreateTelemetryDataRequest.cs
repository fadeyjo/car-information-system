namespace server.Contracts.Requests
{
    public class CreateTelemetryDataRequest
    {
        public DateTime RecDatetime { get; set; }
        public byte ServiceId { get; set; }
        public ushort PID { get; set; }
        public byte[] EcuId { get; set; } = null!;
        public byte ResponseDlc { get; set; }
        public byte[]? Response { get; set; }
        public ulong TripId { get; set; }
    }
}
