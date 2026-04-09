using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Contracts.Responses;
using server.Services.Interfaces;
using server.Utils;
using System.Security.Claims;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class CarsController : ControllerBase
    {
        private readonly ICarsService _service;

        public CarsController(ICarsService service)
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

        [HttpPost]
        public async Task<IActionResult> CreateCar(CreateCarRequest body)
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

                var car = await _service.CreateCar(
                    body.VinNumber, body.StateNumber,
                    body.BrandName, body.ModelName,
                    body.BodyName, body.ReleaseYear,
                    body.GearboxName, body.DriveName,
                    body.VehicleWeightKg, body.EnginePowerHp,
                    body.EnginePowerKw, body.EngineCapacityL,
                    body.TankCapacityL, body.FuelTypeName,
                    personId
                );

                return CreatedAtAction(nameof(GetCarById), new { carId = car.CarId }, car);
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

        [HttpPut("{carId}")]
        public async Task<IActionResult> UpdateCarInfo(UpdateCarInfoRequest body, uint carId)
        {
            try
            {
                await _service.UpdateCarInfo(
                    body.VinNumber, body.StateNumber,
                    body.BrandName, body.ModelName,
                    body.BodyName, body.ReleaseYear,
                    body.GearboxName, body.DriveName,
                    body.VehicleWeightKg, body.EnginePowerHp,
                    body.EnginePowerKw, body.EngineCapacityL,
                    body.TankCapacityL, body.FuelTypeName,
                    carId
                );

                return NoContent();
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

        [HttpGet("{carId}")]
        public async Task<IActionResult> GetCarById(uint carId)
        {
            try
            {
                var car = await _service.GetCarById(carId);

                return Ok(car);
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

        [HttpGet("my-cars")]
        public async Task<IActionResult> GetCarsByPersonId()
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

                var cars = await _service.GetCarsByPersonId(personId);

                return Ok(cars);
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

        [HttpDelete("{carId}")]
        [Authorize(Roles = "OPERATOR")]
        public async Task<IActionResult> DeleteCar(uint carId)
        {
            try
            {
                await _service.DeleteCar(carId);

                return NoContent();
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
