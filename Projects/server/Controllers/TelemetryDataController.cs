using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/telemetry-data")]
    [Authorize]
    public class TelemetryDataController : ControllerBase
    {
        private readonly ITelemetryDataService _service;

        public TelemetryDataController(ITelemetryDataService service)
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
        public async Task<IActionResult> CreateTelemetryData(CreateTelemetryDataRequest body)
        {
            try
            {
                var record = await _service.CreateTelemetryData(
                    body.RecDatetime, body.ServiceId,
                    body.PID, body.EcuId,
                    body.ResponseDlc, body.Response,
                    body.TripId
                );

                return CreatedAtAction(
                    nameof(GetTelemtryDataById),
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
        public async Task<IActionResult> GetTelemtryDataById(ulong recordId)
        {
            try
            {
                var record = await _service.GetTelemtryDataById(recordId);

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
