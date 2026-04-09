using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ITripsService
    {
        Task<TripDto> StartTrip(
            DateTime startDatetime, string macAddress,
            uint carId
        );
        Task<TripDto> GetTripById(ulong tripId);
        Task EndTrip(DateTime endDatetime, ulong tripId);

        Task DeleteTrip(ulong tripId);
    }
}
