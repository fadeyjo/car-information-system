using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarsService
    {
        Task<CarDto> CreateCar(
            string vinNumber, string? stateNumber,
            string brandName, string modelName,
            string bodyName, ushort releaseYear,
            string gearboxName, string driveName,
            ushort vehicleWeightKg, ushort enginePowerHp,
            float enginePowerKw, float engineCapacityL,
            byte tankCapacityL, string fuelTypeName,
            uint personId
        );

        Task UpdateCarInfo(
            string vinNumber, string? stateNumber,
            string brandName, string modelName,
            string bodyName, ushort releaseYear,
            string gearboxName, string driveName,
            ushort vehicleWeightKg, ushort enginePowerHp,
            float enginePowerKw, float engineCapacityL,
            byte tankCapacityL, string fuelTypeName,
            uint carId
        );

        Task<CarDto> GetCarById(uint carId);

        Task<List<CarDto>> GetCarsByPersonId(uint personId);

        Task DeleteCar(uint carId);
    }
}
