using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface IOBDIIPIDSService
    {
        Task<PidsDetailDto> GetCurrentDataPidsDetail(uint suppoortedPids);
    }
}
