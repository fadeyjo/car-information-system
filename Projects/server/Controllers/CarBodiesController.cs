using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/car-bodies")]
    [Authorize]
    public class CarBodiesController : ControllerBase
    {
        private readonly ICarBodiesService _service;

        public CarBodiesController(ICarBodiesService service)
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

        [HttpGet]
        public async Task<IActionResult> GetAllCarBodies()
        {
            try
            {
                var bodies = await _service.GetAllCarBodies();

                return Ok(bodies);
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
