using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarDrivesService
    {
        Task<List<CarDriveDto>> GetAllCarDrives();
    }
}
