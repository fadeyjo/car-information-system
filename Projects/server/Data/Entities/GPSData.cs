namespace server.Models.Entities
{
    public class GPSData
    {
        public ulong RecId { get; set; }
        public DateTime RecDatetime { get; set; }
        public ulong TripId { get; set; }
        public float LatitudeDeg { get; set; }
        public float LongitudeDeg { get; set; }
        public float? AccuracyM { get; set; }
        public uint? SpeedKmh { get; set; }
        public float? BearingDeg { get; set; }
        public Trip Trip { get; set; } = null!;
    }
}
