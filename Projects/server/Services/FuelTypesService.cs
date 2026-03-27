using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;

namespace server.Services
{
    public class FuelTypesService : IFuelTypesService
    {
        private readonly AppDbContext _context;

        public FuelTypesService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<List<FuelTypeDto>> GetAllFuelTypes()
        {
            var types = await _context.FuelTypes.Select(f => new FuelTypeDto()
            {
                TypeName = f.TypeName
            }).ToListAsync();

            return types;
        }
    }
}
