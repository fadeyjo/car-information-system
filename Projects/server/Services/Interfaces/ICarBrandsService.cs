using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarBrandsService
    {
        Task<List<CarBrandDto>> GetAllCarBrands();
    }
}
