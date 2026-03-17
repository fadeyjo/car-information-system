namespace server.Contracts.Responses
{
    public class TelemetryDataDto
    {
        public ulong RecId { get; set; }
        public DateTime RecDatetime { get; set; }
        public byte ServiceId { get; set; }
        public ushort PID { get; set; }
        public string EcuIdBase64 { get; set; } = null!;
        public byte ResponseDlc { get; set; }
        public string? ResponseBase64 { get; set; }
        public ulong TripId { get; set; }
    }
}
