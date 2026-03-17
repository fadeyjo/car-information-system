namespace server.Contracts.Requests
{
    public class EndTripRequest
    {
        public ulong TripId { get; set; }
        public DateTime EndDatetime { get; set; }
    }
}
