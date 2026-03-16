using Microsoft.AspNetCore.Mvc;
using server.Contracts.Requests;
using server.Services.Interfaces;

namespace server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PersonsController : ControllerBase
    {
        private readonly IPersonsService _service;

        public PersonsController(IPersonsService service)
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

        [HttpGet("{id}")]
        public async Task<IActionResult> GetPersonById(uint id)
        {
            try
            {
                var person = await _service.GetPersonById(id);

                return
                    person == null ?
                        Problem(
                            title: "Пользователь не найден",
                            statusCode: StatusCodes.Status404NotFound
                        ) :
                        Ok(person);
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
                bool exists = await _service.PersonExistsByEmail(body.Email);
                if (exists)
                    return Problem(
                        title: "Пользователь с данным email уже существует",
                        statusCode: StatusCodes.Status409Conflict
                    );

                exists = await _service.PersonExistsByPhone(body.Phone);
                if (exists)
                    return Problem(
                        title: "Пользователь с данным номером телефона уже существует",
                        statusCode: StatusCodes.Status409Conflict
                    );

                exists = !string.IsNullOrWhiteSpace(body.DriveLicense) && await _service.PersonExistsByEmail(body.DriveLicense);
                if (exists)
                    return Problem(
                        title: "Пользователь с данным водительским удостоверением уже существует",
                        statusCode: StatusCodes.Status409Conflict
                    );

                var person = await _service.SignUp(body);

                return CreatedAtAction(
                    nameof(GetPersonById),
                    new { id = person.PersonId },
                    person
                );
            }
            catch
            {
                return ServerError();
            }
        }
    }
}
