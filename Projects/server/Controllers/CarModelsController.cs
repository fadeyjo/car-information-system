using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/car-models")]
    [Authorize]
    public class CarModelsController : ControllerBase
    {
        private readonly ICarModelsService _service;

        public CarModelsController(ICarModelsService service)
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

        [HttpGet("{brandName}")]
        public async Task<IActionResult> GetAllModelsByBrandName(string brandName)
        {
            try
            {
                var models = await _service.GetAllCarModelsByBrandName(brandName);

                return Ok(models);
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
