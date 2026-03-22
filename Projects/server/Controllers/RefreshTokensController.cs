using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity.Data;
using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Services.Interfaces;
using server.Utils;
using System.Security.Claims;

namespace server.Controllers
{
    [ApiController]
    [Route("api/refresh-tokens")]
    public class RefreshTokensController : ControllerBase
    {
        private readonly IRefreshTokensService _service;

        public RefreshTokensController(IRefreshTokensService service)
        {
            _service = service;
        }

        private ObjectResult ServerError()
        {
            return Problem(
                title: "Внутрення ошибка сервера",
                statusCode: StatusCodes.Status500InternalServerError
            );
        }

        [HttpPost("login")]
        public async Task<IActionResult> LogIn(LoginRequest body)
        {
            try
            {
                var res = await _service.LogIn(body.Email, body.Password);

                return Ok(res);
            }
            catch (HttpError er)
            {
                return
                    Problem(
                        title: er.Title,
                        statusCode: er.StatusCode
                    );
            }
            catch
            {
                return ServerError();
            }
        }

        [HttpPost("refresh")]
        public async Task<IActionResult> Refresh(RefreshTokensRequest body)
        {
            try
            {
                var res = await _service.Refresh(body.RefreshToken);

                return Ok(res);
            }
            catch (HttpError er)
            {
                return
                    Problem(
                        title: er.Title,
                        statusCode: er.StatusCode
                    );
            }
            catch
            {
                return ServerError();
            }
        }

        [HttpPost("logout")]
        [Authorize]
        public async Task<IActionResult> LogOut()
        {
            try
            {
                var userData = User.FindFirst(ClaimTypes.NameIdentifier);
                if (userData == null)
                    return Problem(
                        title: "Не авторизован",
                        statusCode: StatusCodes.Status401Unauthorized
                    );

                uint personId = uint.Parse(userData.Value);

                var res = await _service.LogOut(personId);

                return Ok(res);
            }
            catch (HttpError er)
            {
                return
                    Problem(
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
