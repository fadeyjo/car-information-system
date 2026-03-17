namespace server.Models.Entities
{
    public class TelemetryData
    {
        public ulong RecId { get; set; }
        public DateTime RecDatetime { get; set; }
        public uint OBDIIPIDId { get; set; }
        public byte[] EcuId { get; set; } = null!;
        public byte ResponseDlc { get; set; }
        public byte[]? Response { get; set; }
        public ulong TripId { get; set; }
        public Trip Trip { get; set; } = null!;
        public OBDIIPID OBDIIPID { get; set; } = null!;
    }
}
