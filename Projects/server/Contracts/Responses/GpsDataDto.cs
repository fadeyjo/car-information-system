namespace server.Contracts.Responses
{
    public class GpsDataDto
    {
        public ulong RecId { get; set; }
        public DateTime RecDatetime { get; set; }
        public ulong TripId { get; set; }
        public float LatitudeDeg { get; set; }
        public float LongitudeDeg { get; set; }
        public float? AccuracyM { get; set; }
        public uint? SpeedKmh { get; set; }
        public float? BearingDeg { get; set; }
    }
}
