using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;
using System;

namespace server.Services
{
    public class TelemetryDataService : ITelemetryDataService
    {
        private readonly AppDbContext _context;

        public TelemetryDataService(AppDbContext context)
        {
            _context=context;
        }

        public async Task<TelemetryDataDto> CreateTelemetryData(
            DateTime recDatetime, byte serviceId,
            ushort PID, byte[] EcuId,
            byte responseDlc, byte[]? response,
            ulong tripId
        )
        {
            bool exists = await _context.Trips.AnyAsync(t => t.TripId == tripId);

            if (!exists)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            var OBDIIPID =
                await _context.OBDIIPIDs
                    .FirstOrDefaultAsync(p =>
                        p.ServiceId == serviceId &&
                        p.PID == PID
                    );

            if (OBDIIPID == null)
            {
                exists =
                    await _context.OBDIIServices.AnyAsync(s => s.ServiceId == serviceId);

                if (!exists)
                {
                    var newService = new OBDIIService()
                    {
                        ServiceId = serviceId,
                        ServiceDescription = "Неизвестный сервис"
                    };
                    _context.OBDIIServices.Add(newService);
                    await _context.SaveChangesAsync();

                    OBDIIPID = new OBDIIPID()
                    {
                        ServiceId = serviceId,
                        PID = PID,
                        PIDDescription = "Неизвестный PID"
                    };
                    _context.OBDIIPIDs.Add(OBDIIPID);
                    await _context.SaveChangesAsync();
                }
            }

            if (OBDIIPID == null)
                throw new Exception();

            var record = new TelemetryData()
            {
                RecDatetime = recDatetime,
                OBDIIPIDId = OBDIIPID.OBDIIPIDId,
                EcuId = EcuId,
                ResponseDlc = responseDlc,
                Response = response,
                TripId = tripId
            };

            _context.TelemetryData.Add(record);
            await _context.SaveChangesAsync();

            return new TelemetryDataDto()
            {
                RecId = record.RecId,
                RecDatetime = record.RecDatetime,
                ServiceId = OBDIIPID.ServiceId,
                PID = OBDIIPID.PID,
                EcuIdBase64 = Convert.ToBase64String(EcuId),
                ResponseDlc = responseDlc,
                ResponseBase64 =
                    response != null ?
                        Convert.ToBase64String(response):
                        null,
                TripId = tripId
            };
        }

        public async Task<TelemetryDataDto> GetTelemtryDataById(ulong recordId)
        {
            var record =
                await _context.TelemetryData
                    .Where(t => t.RecId == recordId)
                    .Include(t => t.OBDIIPID)
                    .Select(t => new TelemetryDataDto()
                    {
                        RecId = t.RecId,
                        RecDatetime = t.RecDatetime,
                        ServiceId = t.OBDIIPID.ServiceId,
                        PID = t.OBDIIPID.PID,
                        EcuIdBase64 = Convert.ToBase64String(t.EcuId),
                        ResponseDlc = t.ResponseDlc,
                        ResponseBase64 =
                            t.Response != null ?
                                Convert.ToBase64String(t.Response) :
                                null,
                        TripId = t.TripId
                    })
                    .FirstOrDefaultAsync();

            if (record == null)
                throw new HttpError("Запись не найдена", StatusCodes.Status404NotFound);

            return record;
        }
    }
}
