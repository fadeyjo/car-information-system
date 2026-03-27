using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;
using server.Utils;

namespace server.Services
{
    public class CarModelsService : ICarModelsService
    {
        private readonly AppDbContext _context;

        public CarModelsService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<List<CarModelDto>> GetAllCarModelsByBrandName(string brandName)
        {
            bool exists = await _context.CarBrands.AnyAsync(b => b.BrandName == brandName);

            if (!exists)
                throw new HttpError("Брэнд не найден", StatusCodes.Status404NotFound);

            var models = await _context.CarBrandsModels
                .Include(m => m.CarBrand)
                .Select(m => new CarModelDto()
                {
                    BrandId = m.CarBrand.BrandId,
                    ModelName = m.ModelName
                }).ToListAsync();

            return models;
        }
    }
}
