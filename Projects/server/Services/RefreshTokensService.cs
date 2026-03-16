using Microsoft.EntityFrameworkCore;
using server.Auth;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;
using System;

namespace server.Services
{
    public class RefreshTokensService : IRefreshTokensService
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _configuration;
        private readonly JwtService _jwtService;

        public RefreshTokensService(AppDbContext context, IConfiguration configuration, JwtService jwtService)
        {
            _context = context;
            _configuration = configuration;
            _jwtService = jwtService;
        }

        public async Task<TokensDto> LogIn(string email, string password)
        {
            var person = await _context.Persons.Include(p => p.Role).FirstOrDefaultAsync(p => p.Email == email);

            if (person == null)
                throw new HttpError("Пользователь не зарегистрирован", StatusCodes.Status404NotFound);

            if (!BCrypt.Net.BCrypt.Verify(password, person.HashedPassword))
                throw new HttpError("Неправильный пароль", StatusCodes.Status401Unauthorized);

            string refreshToken;
            string hashedRefreshToken;
            do
            {
                refreshToken = _jwtService.GenerateRefreshToken();
                hashedRefreshToken = BCrypt.Net.BCrypt.HashPassword(refreshToken);
            }
            while (await RefreshTokenHashExists(hashedRefreshToken));

            int refreshTokenDays = _configuration.GetValue<int>("Jwt:RefreshTokenDays");

            var newRefreshToken = new RefreshToken()
            {
                TokenHash = hashedRefreshToken,
                Expires = DateTime.UtcNow.AddDays(refreshTokenDays),
                IsRevoked = false,
                PersonId = person.PersonId
            };
            
            _context.RefreshTokens.Add(newRefreshToken);
            await _context.SaveChangesAsync();

            string accessToken = _jwtService.GenerateAccessToken(person.PersonId, person.Role.RoleName);

            var tokens = new TokensDto()
            {
                AccessToken = accessToken,
                RefreshToken = refreshToken
            };

            return tokens;
        }

        public async Task<LogOutDto> LogOut(uint personId)
        {
            bool exists = await _context.Persons.AnyAsync(p => p.PersonId == personId);

            if (!exists)
                throw new HttpError("Пользователь не найден", StatusCodes.Status404NotFound);

            var refreshTokens =
                await _context.RefreshTokens.Where(rt => rt.PersonId == personId).ToListAsync();

            for (int i = 0; i  < refreshTokens.Count; i++)
                refreshTokens[i].IsRevoked = true;

            await _context.SaveChangesAsync();

            return
                new LogOutDto()
                {
                    PersonId = personId
                };
        }

        public async Task<TokensDto> Refresh(string oldRefreshToken)
        {
            var tokens =
                await _context.RefreshTokens
                    .Where(r => r.Expires > DateTime.UtcNow && !r.IsRevoked)
                    .Include(r => r.Person)
                    .Include(r => r.Person.Role)
                    .ToListAsync();

            var matchingToken = tokens
                .FirstOrDefault(t => BCrypt.Net.BCrypt.Verify(oldRefreshToken, t.TokenHash));

            if (matchingToken == null)
                throw new HttpError("Не авторизован", StatusCodes.Status401Unauthorized);

            matchingToken.IsRevoked = true;
            await _context.SaveChangesAsync();

            string refreshToken;
            string hashedRefreshToken;
            do
            {
                refreshToken = _jwtService.GenerateRefreshToken();
                hashedRefreshToken = BCrypt.Net.BCrypt.HashPassword(refreshToken);
            }
            while (await RefreshTokenHashExists(hashedRefreshToken));

            int refreshTokenDays = _configuration.GetValue<int>("Jwt:RefreshTokenDays");

            var newRefreshToken = new RefreshToken()
            {
                TokenHash = hashedRefreshToken,
                Expires = DateTime.UtcNow.AddDays(refreshTokenDays),
                IsRevoked = false,
                PersonId = matchingToken.Person.PersonId
            };

            _context.RefreshTokens.Add(newRefreshToken);
            await _context.SaveChangesAsync();

            string accessToken =
                _jwtService.GenerateAccessToken(
                    matchingToken.Person.PersonId,
                    matchingToken.Person.Role.RoleName
                );

            var newTokens = new TokensDto()
            {
                AccessToken = accessToken,
                RefreshToken = refreshToken
            };

            return newTokens;
        }

        public async Task<bool> RefreshTokenHashExists(string hashedRefreshToken)
        {
            return
                await _context.RefreshTokens.AnyAsync(rt => rt.TokenHash == hashedRefreshToken);
        }
    }
}
