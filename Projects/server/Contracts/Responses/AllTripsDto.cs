namespace server.Contracts.Responses
{
    public class AllTripsDto
    {
        public List<TripDto> current { get; set; } = null!;
        public List<TripDto> ended { get; set; } = null!;
    }
}
