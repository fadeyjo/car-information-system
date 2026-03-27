using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;

namespace server.Services
{
    public class CarBodiesService : ICarBodiesService
    {
        private readonly AppDbContext _context;

        public CarBodiesService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<List<CarBodyDto>> GetAllCarBodies()
        {
            var bodies = await _context.CarBodies.Select(b => new CarBodyDto()
            {
                BodyName = b.BodyName
            }).ToListAsync();

            return bodies;
        }
    }
}
