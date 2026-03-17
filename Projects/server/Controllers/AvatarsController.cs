using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Services.Interfaces;
using server.Utils;
using System.Security.Claims;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class AvatarsController : ControllerBase
    {
        private readonly IAvatarsService _service;

        public AvatarsController(IAvatarsService service)
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

        [HttpPost]
        public async Task<IActionResult> CreateAvatar(IFormFile file)
        {
            try
            {
                var userData = User.FindFirst(ClaimTypes.NameIdentifier);

                if (userData == null)
                    return Problem(
                        title: "Не авторизован",
                        statusCode: StatusCodes.Status401Unauthorized
                    );

                if (file == null)
                    return Problem(
                        title: "Файл обязателен",
                        statusCode: StatusCodes.Status400BadRequest
                    );

                var personId = uint.Parse(userData.Value);

                var avatar = await _service.CreateAvatar(file, personId);

                return CreatedAtAction(
                    nameof(GetAvatarById),
                    new { avatarId = avatar.AvatarId },
                    avatar
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

        [HttpGet("{avatarId}")]
        public async Task<IActionResult> GetAvatarById(uint avatarId)
        {
            try
            {
                (var bytes, var contentType, var fileName) = await _service.GetAvatarById(avatarId);

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
