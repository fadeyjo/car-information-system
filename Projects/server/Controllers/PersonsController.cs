using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PersonsController : ControllerBase
    {
        private readonly IPersonsService _service;

        public PersonsController(IPersonsService service)
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

        [HttpGet("{id}")]
        [Authorize]
        public async Task<IActionResult> GetPersonById(uint id)
        {
            try
            {
                var person = await _service.GetPersonById(id);

                return Ok(person);
            }
            catch
            {
                return ServerError();
            }
        }

        [HttpPost]
        public async Task<IActionResult> SignUp(SignUpRequest body)
        {
            try
            {
                var res = await _service.SignUp(body);

                return CreatedAtAction(
                    nameof(GetPersonById),
                    new { id = res.PersonId },
                    res
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
    }
}
