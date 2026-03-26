using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;

namespace server.Services
{
    public class AvatarsService : IAvatarsService
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _configuration;

        public AvatarsService(AppDbContext context, IConfiguration configuration)
        {
            _context=context;
            _configuration=configuration;
        }

        public async Task<AvatarDto> CreateAvatar(IFormFile file, uint personid)
        {
            if (file == null || file.Length == 0)
                throw new HttpError("Файл обязателен", StatusCodes.Status400BadRequest);

            var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".webp", ".gif" };

            var ext = Path.GetExtension(file.FileName);

            if (!allowedExtensions.Contains(ext.ToLower()))
                throw new HttpError("Недопустимый формат файла", 400);

            bool exists = await _context.Persons.AnyAsync(p => p.PersonId == personid);

            if (!exists)
                throw new HttpError("Пользователь не найден", StatusCodes.Status404NotFound);

            string basePath = _configuration.GetValue<string>("Store:AvatarsPath").Replace("\\", "/");

            if (string.IsNullOrWhiteSpace(basePath))
                throw new Exception();

            string personFolder = Path.Combine(basePath, personid.ToString()).Replace("\\", "/");

            if (!Directory.Exists(personFolder))
                Directory.CreateDirectory(personFolder);

            var uniqueFileName = Guid.NewGuid().ToString() + ext;
            var filePath = Path.Combine(personFolder, uniqueFileName).Replace("\\", "/");

            using (var stream = new FileStream(filePath, FileMode.Create))
                await file.CopyToAsync(stream);

            string contentType = GetFileContentType(ext);

            var avatar = new Avatar()
            {
                CreatedAt = DateTime.UtcNow,
                AvatarUrl = $"{personid}/{uniqueFileName}",
                PersonId = personid,
                ContentType = contentType
            };

            _context.Avatars.Add(avatar);
            await _context.SaveChangesAsync();

            return new AvatarDto()
            {
                AvatarId = avatar.AvatarId,
                AvatarUrl = avatar.AvatarUrl,
                PersonId = personid,
                ContentType = contentType
            };
        }

        public async Task<(byte[] bytes, string contentType, string fileName)> GetAvatarById(uint avatarId)
        {
            var avatarData =
                await _context.Avatars
                    .Where(a => a.AvatarId == avatarId)
                    .Select(a => new
                    {
                        a.AvatarUrl,
                        a.ContentType
                    })
                    .FirstOrDefaultAsync();

            if (avatarData == null)
                throw new HttpError("Аватар не найден", StatusCodes.Status404NotFound);

            var basePath = _configuration.GetValue<string>("Store:AvatarsPath").Replace("\\", "/");

            if (string.IsNullOrWhiteSpace(basePath))
                throw new Exception();

            var avatarPath = Path.Combine(basePath, avatarData.AvatarUrl).Replace("\\", "/");
            Console.Write(avatarPath);

            if (!File.Exists(avatarPath))
                throw new HttpError("Аватар не найден", StatusCodes.Status404NotFound);

            var fileBytes = await File.ReadAllBytesAsync(avatarPath);

            return (fileBytes, avatarData.ContentType, Path.GetFileName(avatarPath));
        }

        private string GetFileContentType(string ext)
        {
            switch (ext)
            {
                case ".jpeg":
                    return "image/jpeg";
                case ".jpg":
                    return "image/jpeg";
                case ".png":
                    return "image/png";
                case ".webp":
                    return "image/webp";
                case ".gif":
                    return "image/gif";
                case ".svg":
                    return "image/svg";
                case ".tiff":
                    return "image/tiff";
                case ".tif":
                    return "image/tif";
                case ".bmp":
                    return "image/bmp";
                case ".heic":
                    return "image/heic";
                case ".heif":
                    return "image/heif";
                default:
                    return "image/psd";
            }
        }
    }
}
