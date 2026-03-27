using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;

namespace server.Services
{
    public class CarGearboxesService : ICarGearboxesService
    {
        private readonly AppDbContext _context;

        public CarGearboxesService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<List<CarGearboxDto>> GetAllCarGearboxes()
        {
            var gearboxes = await _context.CarGearboxes.Select(g => new CarGearboxDto()
            {
                GearboxName = g.GearboxName
            }).ToListAsync();

            return gearboxes;
        }
    }
}
