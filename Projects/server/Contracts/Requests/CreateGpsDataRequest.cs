namespace server.Contracts.Requests
{
    public class CreateGpsDataRequest
    {
        public DateTime RecDateTime { get; set; }
        public ulong TripId { get; set; }
        public float LatitudeDeg { get; set; }
        public float LongitudeDeg { get; set; }
        public float? AccuracyM { get; set; }
        public uint? SpeedKmh { get; set; }
        public float? BearingDeg { get; set; }
    }
}
