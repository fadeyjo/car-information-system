namespace server.Contracts.Requests
{
    public class StartTripRequest
    {
        public DateTime StartDatetime { get; set; }
        public string MacAddress { get; set; } = null!;
        public uint CarId { get; set; }
    }
}
