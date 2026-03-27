using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;

namespace server.Services
{
    public class CarDrivesService : ICarDrivesService
    {
        private readonly AppDbContext _context;

        public CarDrivesService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<List<CarDriveDto>> GetAllCarDrives()
        {
            var drives = await _context.CarDrives.Select(d => new CarDriveDto()
            {
                DriveName = d.DriveName
            }).ToListAsync();

            return drives;
        }
    }
}
