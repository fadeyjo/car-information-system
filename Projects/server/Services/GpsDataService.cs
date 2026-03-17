using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;

namespace server.Services
{
    public class GpsDataService : IGpsDataService
    {
        private readonly AppDbContext _context;

        public GpsDataService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<GpsDataDto> CreateGpsData(
            DateTime recDateTime, ulong tripId,
            float latitudeDeg, float longitudeDeg,
            float? accuracyM, uint? speedKmh,
            ushort? bearingDeg
        )
        {
            bool exist =
                await _context.Trips.AnyAsync(g => g.TripId == tripId);

            if (!exist)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            var record = new GPSData()
            {
                RecDatetime = recDateTime,
                TripId = tripId,
                LatitudeDeg = latitudeDeg,
                LongitudeDeg = longitudeDeg,
                AccuracyM = accuracyM,
                SpeedKmh = speedKmh,
                BearingDeg = bearingDeg
            };
            _context.GPSData.Add(record);
            await _context.SaveChangesAsync();

            return new GpsDataDto()
            {
                RecId = record.RecId,
                RecDatetime = recDateTime,
                TripId = tripId,
                LatitudeDeg = latitudeDeg,
                LongitudeDeg = longitudeDeg,
                AccuracyM = accuracyM,
                SpeedKmh = speedKmh,
                BearingDeg = bearingDeg
            };
        }

        public async Task<GpsDataDto> GetGpsDataById(ulong recordId)
        {
            var record =
                await _context.GPSData
                    .Where(g => g.RecId == recordId)
                    .Select(g => new GpsDataDto()
                    {
                        RecId = recordId,
                        RecDatetime = g.RecDatetime,
                        TripId = g.TripId,
                        LatitudeDeg = g.LatitudeDeg,
                        LongitudeDeg = g.LongitudeDeg,
                        AccuracyM = g.AccuracyM,
                        SpeedKmh = g.SpeedKmh,
                        BearingDeg = g.BearingDeg

                    })
                    .FirstOrDefaultAsync();

            if (record == null)
                throw new HttpError("Запись не найдена", StatusCodes.Status404NotFound);
            
            return record;
        }
    }
}
