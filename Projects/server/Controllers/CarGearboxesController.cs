using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/car-gearboxes")]
    [Authorize]
    public class CarGearboxesController : ControllerBase
    {
        private readonly ICarGearboxesService _service;

        public CarGearboxesController(ICarGearboxesService service)
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
        public async Task<IActionResult> GetAllCarGearboxes()
        {
            try
            {
                var gearboxes = await _service.GetAllCarGearboxes();

                return Ok(gearboxes);
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
