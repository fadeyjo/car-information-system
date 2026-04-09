using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Contracts.Responses;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class TripsController : ControllerBase
    {
        private readonly ITripsService _service;

        public TripsController(ITripsService service)
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

        [HttpPost("start")]
        public async Task<IActionResult> StartTrip(StartTripRequest body)
        {
            try
            {
                var trip = await _service.StartTrip(
                    body.StartDatetime,
                    body.MacAddress,
                    body.CarId
                );

                return CreatedAtAction(
                    nameof(GetTripById),
                    new { tripId = trip.TripId },
                    trip
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

        [HttpGet("{tripId}")]
        public async Task<IActionResult> GetTripById(ulong tripId)
        {
            try
            {
                var trip = await _service.GetTripById(tripId);

                return Ok(trip);
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

        [HttpPut("end")]
        public async Task<IActionResult> EndTrip(EndTripRequest body)
        {
            try
            {
                await _service.EndTrip(body.EndDatetime, body.TripId);

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

        [HttpDelete("{tripId}")]
        public async Task<IActionResult> DeleteTrip(ulong tripId)
        {
            try
            {
                await _service.DeleteTrip(tripId);

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
