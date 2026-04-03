using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/obdii-pids")]
    [Authorize]
    public class OBDIIPIDSController : ControllerBase
    {
        private readonly IOBDIIPIDSService _service;

        public OBDIIPIDSController(IOBDIIPIDSService service)
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

        [HttpGet("current-data-service/{supportedPidsUint}")]
        public async Task<IActionResult> GetCurrentDataPidsDetail(uint supportedPidsUint)
        {
            try
            {
                var res = await _service.GetCurrentDataPidsDetail(supportedPidsUint);

                return Ok(res);
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
