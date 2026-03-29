using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarBrandsService
    {
        Task<List<CarBrandDto>> GetAllCarBrands();
        Task<List<CarBrandDto>> GetAllCarBrandsByText(string text);
    }
}
