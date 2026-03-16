using server.Contracts.Responses;
using server.Models.Entities;

namespace server.Services.Interfaces
{
    public interface IRefreshTokensService
    {
        Task<TokensDto> LogIn(string email, string password);
        Task<TokensDto> Refresh(string oldRefreshToken);
        Task<LogOutDto> LogOut(uint personId);
        Task<bool> RefreshTokenHashExists(string hashedRefreshToken);
    }
}
