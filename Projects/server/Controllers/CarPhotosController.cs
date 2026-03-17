using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class CarPhotosController : ControllerBase
    {
        private readonly ICarPhotosService _service;

        public CarPhotosController(ICarPhotosService service)
        {
            _service=service;
        }

        private ObjectResult ServerError()
        {
            return Problem(
                title: "Внутрення ошибка сервера",
                statusCode: StatusCodes.Status500InternalServerError
            );
        }

        [HttpPost("{carId}")]
        public async Task<IActionResult> CreateCarPhoto(IFormFile file, uint carId)
        {
            try
            {
                if (file == null)
                    return Problem(
                        title: "Файл обязателен",
                        statusCode: StatusCodes.Status400BadRequest
                    );

                var carPhoto = await _service.CreateCarPhoto(file, carId);

                return CreatedAtAction(
                    nameof(GetCarPhotoById),
                    new { photoId = carPhoto.PhotoId },
                    carPhoto
                );
            }
            catch (HttpError er)
            {
                return Problem(
                    title: er.Title,
                    statusCode: er.StatusCode
                );
            }
            catch
            {
                return ServerError();
            }
        }

        [HttpGet("{photoId}")]
        public async Task<IActionResult> GetCarPhotoById(uint photoId)
        {
            try
            {
                (var bytes, var contentType, var fileName) = await _service.GetCarPhotoById(photoId);

                return File(bytes, contentType, fileName);
            }
            catch (HttpError er)
            {
                return Problem(
                    title: er.Title,
                    statusCode: er.StatusCode
                );
            }
            catch
            {
                return ServerError();
            }
        }
    }
}
