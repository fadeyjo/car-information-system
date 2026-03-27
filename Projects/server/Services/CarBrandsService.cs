using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;

namespace server.Services
{
    public class CarBrandsService : ICarBrandsService
    {
        private readonly AppDbContext _context;

        public CarBrandsService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<List<CarBrandDto>> GetAllCarBrands()
        {
            var brands = await _context.CarBrands.Select(b => new CarBrandDto()
            {
                BrandName = b.BrandName
            }).ToListAsync();

            return brands;
        }
    }
}
