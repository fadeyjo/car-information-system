using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarPhotosService
    {
        Task<CarPhotoDto> CreateCarPhoto(IFormFile file, uint carId);
        Task<(byte[] bytes, string contentType, string fileName)> GetCarPhotoById(uint photoId);
    }
}
