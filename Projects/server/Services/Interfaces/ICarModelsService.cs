using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarModelsService
    {
        Task<List<CarModelDto>> GetAllCarModelsByBrandName(string brandName);
        Task<List<CarModelDto>> GetAllCarModelsByText(string text, string brandName);
    }
}
