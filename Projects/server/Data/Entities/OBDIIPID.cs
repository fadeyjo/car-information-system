namespace server.Models.Entities
{
    public class OBDIIPID
    {
        public uint OBDIIPIDId { get; set; }
        public byte ServiceId { get; set; }
        public ushort PID { get; set; }
        public string PIDDescription { get; set; } = null!;
        public OBDIIService OBDIIService { get; set; } = null!;
    }
}
