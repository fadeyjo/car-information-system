using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;
using System.Security.Claims;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
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

        [HttpGet]
        public async Task<IActionResult> GetPersonById()
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

                var person = await _service.GetPersonById(personId);

                return Ok(person);
            }
            catch
            {
                return ServerError();
            }
        }

        [HttpPost]
        [AllowAnonymous]
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

        [HttpPut]
        public async Task<IActionResult> UpdatePersonInfo(UpdatePersonInfoRequest body)
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

                await _service.UpdatePersonInfo(body, personId);

                return NoContent();
            }
            catch
            {
                return ServerError();
            }
        }
    }
}
