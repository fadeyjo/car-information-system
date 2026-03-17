using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface IAvatarsService
    {
        Task<AvatarDto> CreateAvatar(IFormFile file, uint personid);
        Task<(byte[] bytes, string contentType, string fileName)> GetAvatarById(uint avatarId);
    }
}
