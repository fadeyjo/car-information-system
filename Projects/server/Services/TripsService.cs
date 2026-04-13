using Microsoft.EntityFrameworkCore;
using server.Contracts.Requests;
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

        private readonly IOBDIIPIDSService _service;

        public TripsService(AppDbContext context, IOBDIIPIDSService service)
        {
            _context = context;
            _service = service;
        }

        public async Task DeleteTrip(ulong tripId)
        {
            var trip = await _context.Trips.FirstOrDefaultAsync(t => t.TripId == tripId);

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            _context.Trips.Remove(trip);
            await _context.SaveChangesAsync();
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

        private byte[] GetRawResponse(uint sup)
        {
            // Со старших до младших

            var response = new byte[8];
            response[0] = 7;
            response[1] = 0x41;
            response[2] = 0;
            response[3] = (byte)(sup >> 24 & 0xFF);
            response[4] = (byte)(sup >> 16 & 0xFF);
            response[5] = (byte)(sup >> 8 & 0xFF);
            response[6] = (byte)(sup & 0xFF);
            response[7] = 0;

            return response;
        }

        public async Task<TripDto> StartTrip(DateTime startDatetime, string macAddress, uint carId, byte[] ECUId, uint supported)
        {
            macAddress = macAddress.ToUpper();

            bool exits = await _context.Cars.AnyAsync(c => c.CarId == carId);

            if (!exits)
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);

            var obdIIId = await _service.GetOBDIIPIDId(1, 0);

            if (obdIIId == null)
                throw new HttpError("PID не найден", StatusCodes.Status404NotFound);

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

            var supportedPidsData = new TelemetryData()
            {
                RecDatetime = startDatetime,
                OBDIIPIDId = (uint)obdIIId,
                EcuId = ECUId,
                ResponseDlc = 8,
                Response = GetRawResponse(supported),
                TripId = newTrip.TripId
            };

            _context.TelemetryData.Add(supportedPidsData);
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
