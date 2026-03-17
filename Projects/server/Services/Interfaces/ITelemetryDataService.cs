using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ITelemetryDataService
    {
        Task<TelemetryDataDto> CreateTelemetryData(
            DateTime recDatetime, byte serviceId,
            ushort PID, byte[] EcuId,
            byte responseDlc, byte[]? response,
            ulong tripId
        );

        Task<TelemetryDataDto> GetTelemtryDataById(ulong recordId);
    }
}
