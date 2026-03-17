using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/gps-data")]
    public class GpsDataController : ControllerBase
    {
        private readonly IGpsDataService _service;

        public GpsDataController(IGpsDataService service)
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
        public async Task<IActionResult> CreateGpsData(CreateGpsDataRequest body)
        {
            try
            {
                var record = await _service.CreateGpsData(
                    body.RecDateTime, body.TripId,
                    body.LatitudeDeg, body.LongitudeDeg,
                    body.AccuracyM, body.SpeedKmh,
                    body.BearingDeg
                );

                return CreatedAtAction(
                    nameof(GetGpsDataById),
                    new { recordId = record.RecId },
                    record
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

        [HttpGet("{recordId}")]
        public async Task<IActionResult> GetGpsDataById(ulong recordId)
        {
            try
            {
                var record = await _service.GetGpsDataById(recordId);

                return Ok(record);
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
