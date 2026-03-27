using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface IFuelTypesService
    {
        Task<List<FuelTypeDto>> GetAllFuelTypes();
    }
}
