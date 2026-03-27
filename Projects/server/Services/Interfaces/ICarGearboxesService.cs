using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarGearboxesService
    {
        Task<List<CarGearboxDto>> GetAllCarGearboxes();
    }
}
