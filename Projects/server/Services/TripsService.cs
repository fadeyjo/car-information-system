using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;

namespace server.Services
{
    public class TripsService : ITripsService
    {
        private readonly AppDbContext _context;

        public TripsService(AppDbContext context)
        {
            _context = context;
        }

        public async Task EndTrip(DateTime endDatetime, ulong tripId)
        {
            var trip =
                await _context.Trips.FirstOrDefaultAsync(t => t.TripId == tripId);

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            if (trip.EndDatetime != null)
                throw new HttpError("Поездка уже закончена", StatusCodes.Status409Conflict);

            if (endDatetime <= trip.StartDatetime)
                throw new HttpError(
                    "Дата и время окончания поездки должны быть позже даты и времени начала поездки",
                    StatusCodes.Status409Conflict
                );

            trip.EndDatetime = endDatetime;
            await _context.SaveChangesAsync();
        }

        public async Task<TripDto> GetTripById(ulong tripId)
        {
            var trip =
                await _context.Trips
                    .Where(t => t.TripId == tripId)
                    .Select(t => new TripDto()
                    {
                        TripId = t.TripId,
                        StartDatetime = t.StartDatetime,
                        DeviceId = t.DeviceId,
                        CarId = t.CarId,
                        EndDatetime = t.EndDatetime
                    })
                    .FirstOrDefaultAsync();

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            return trip;
        }

        public async Task<TripDto> StartTrip(DateTime startDatetime, string macAddress, uint carId)
        {
            bool exits = await _context.Cars.AnyAsync(c => c.CarId == carId);

            if (!exits)
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);

            var device = await _context.OBDIIDevices.FirstOrDefaultAsync(d => d.MacAddress == macAddress);

            if (device == null)
            {
                device = new OBDIIDevice()
                {
                    CreatedAt = DateTime.UtcNow,
                    MacAddress = macAddress
                };
                _context.OBDIIDevices.Add(device);
                await _context.SaveChangesAsync();
            }

            var newTrip = new Trip()
            {
                StartDatetime = startDatetime,
                DeviceId = device.DeviceId,
                CarId = carId
            };
            _context.Trips.Add(newTrip);
            await _context.SaveChangesAsync();

            return new TripDto()
            {
                TripId = newTrip.TripId,
                StartDatetime = startDatetime,
                DeviceId = device.DeviceId,
                CarId = carId
            };
        }
    }
}
