using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;

namespace server.Services
{
    public class CarPhotosService : ICarPhotosService
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _configuration;

        public CarPhotosService(AppDbContext context, IConfiguration configuration)
        {
            _context=context;
            _configuration=configuration;
        }

        public async Task<CarPhotoDto> CreateCarPhoto(IFormFile file, uint carId)
        {
            if (file == null)
                throw new HttpError("Файл обязателен", StatusCodes.Status400BadRequest);

            var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".webp", ".gif" };

            var ext = Path.GetExtension(file.FileName);

            bool exists =
                await _context.Cars.AnyAsync(c => c.CarId == carId);

            if (!exists)
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);

            string basePath = _configuration.GetValue<string>("Store:CarPhotosPath");

            if (string.IsNullOrWhiteSpace(basePath))
                throw new Exception();

            var carFolder = Path.Combine(basePath, carId.ToString());

            if (!Directory.Exists(carFolder))
                Directory.CreateDirectory(carFolder);

            var uniqueFileName = Guid.NewGuid().ToString() + ext;
            var filePath = Path.Combine(carFolder, uniqueFileName);

            using (var stream = new FileStream(filePath, FileMode.Create))
                await file.CopyToAsync(stream);

            string contentType = GetFileContentType(ext);

            var photo = new CarPhoto()
            {
                CreatedAt = DateTime.UtcNow,
                PhotoUrl = $"{carId}/{uniqueFileName}",
                CarId = carId,
                ContentType = contentType
            };
            _context.CarPhotos.Add(photo);
            await _context.SaveChangesAsync();

            return new CarPhotoDto()
            {
                PhotoId = photo.PhotoId,
                PhotoUrl = photo.PhotoUrl,
                CarId = carId,
                ContentType = contentType
            };
        }

        public async Task<(byte[] bytes, string contentType, string fileName)> GetCarPhotoById(uint photoId)
        {
            var photoData =
                await _context.CarPhotos
                    .Where(a => a.PhotoId == photoId)
                    .Select(a => new
                    {
                        a.PhotoUrl,
                        a.ContentType
                    })
                    .FirstOrDefaultAsync();

            if (photoData == null)
                throw new HttpError("Фото не найдено", StatusCodes.Status404NotFound);

            var basePath = _configuration.GetValue<string>("Store:CarPhotosPath");

            if (string.IsNullOrWhiteSpace(basePath))
                throw new Exception();

            var photoPath = Path.Combine(basePath, photoData.PhotoUrl);

            if (!File.Exists(photoPath))
                throw new HttpError("Фото не найдено", StatusCodes.Status404NotFound);

            var fileBytes = await File.ReadAllBytesAsync(photoPath);

            return (fileBytes, photoData.ContentType, Path.GetFileName(photoPath));
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
