using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface IGpsDataService
    {
        Task<GpsDataDto> CreateGpsData(
            DateTime recDateTime, ulong tripId,
            float latitudeDeg, float longitudeDeg,
            float? accuracyM, uint? speedKmh,
            float? bearingDeg
        );
        Task<GpsDataDto> GetGpsDataById(ulong recordId);
    }
}
