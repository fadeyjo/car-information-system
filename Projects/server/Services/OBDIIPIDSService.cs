using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;
using System.Linq;
using System.Threading.Tasks.Dataflow;

namespace server.Services
{
    public class OBDIIPIDSService : IOBDIIPIDSService
    {
        private readonly AppDbContext _context;

        public OBDIIPIDSService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<PidsDetailDto> GetCurrentDataPidsDetail(uint suppoortedPidsUint)
        {
            // Со старших разрядов до младших
            var supportedPids = GetSupportedPids(suppoortedPidsUint);

            var once = await _context.OBDIIPIDs.Where(p => p.Once && supportedPids.Contains(p.PID)).Select(p => p.PID).ToListAsync();
            var repeatable = await _context.OBDIIPIDs.Where(p => !p.Once && supportedPids.Contains(p.PID)).Select(p => p.PID).ToListAsync();

            return new PidsDetailDto()
            {
                Once = once,
                Repeatable = repeatable,
            };
        }

        public async Task<uint?> GetOBDIIPIDId(byte serviceId, ushort PID)
        {
            var data = await _context.OBDIIPIDs.Where(o => o.ServiceId == serviceId && o.PID == PID).FirstOrDefaultAsync();

            return data == null ? null : data.OBDIIPIDId;
        }

        private List<ushort> GetSupportedPids(uint pids)
        {
            uint mask = 0x80000000;
            uint filter = 0x80000000;

            List<ushort> result = new List<ushort>();

            for (int i = 0; i < 31; i++)
            {
                if (((pids << i) & mask) == filter)
                {
                    result.Add(Convert.ToUInt16(i + 1));
                }
            }

            return result;
        }
    }
}
