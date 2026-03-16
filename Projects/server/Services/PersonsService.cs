using Microsoft.EntityFrameworkCore;
using server.Contracts.Requests;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;
using server.Utils;
using System.Numerics;

namespace server.Services
{
    public class PersonsService : IPersonsService
    {
        private readonly AppDbContext _context;
        
        public PersonsService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<PersonDto> GetPersonById(uint personId)
        {
            var person =
                await _context.Persons
                    .Select(p => new PersonDto()
                    {
                        PersonId = p.PersonId,
                        Email = p.Email,
                        Phone = p.Phone,
                        LastName = p.LastName,
                        FirstName = p.FirstName,
                        Patronymic = p.Patronymic,
                        Birth = p.Birth,
                        DriveLicense = p.DriveLicense,
                        RoleId = p.RoleId
                    })
                    .Where(p => p.PersonId == personId)
                    .FirstOrDefaultAsync();

            if (person == null)
                throw new HttpError("Пользователь не найден", StatusCodes.Status404NotFound);

            return person;
        }

        public async Task<PersonDto> SignUp(SignUpRequest body)
        {
            bool exists = await _context.Persons.AnyAsync(p => p.Email == body.Email);
            if (exists)
                throw new HttpError("Пользователь с данным email уже существует", StatusCodes.Status409Conflict);

            exists = await _context.Persons.AnyAsync(p => p.Phone == body.Phone);
            if (exists)
                throw new HttpError("Пользователь с данным номером телефона уже существует", StatusCodes.Status409Conflict);

            exists = !string.IsNullOrWhiteSpace(body.DriveLicense) && await _context.Persons.AnyAsync(p => p.DriveLicense == body.DriveLicense);
            if (exists)
                throw new HttpError("Пользователь с данным водительским удостоверением уже существует", StatusCodes.Status409Conflict);

            string hashedPassword = BCrypt.Net.BCrypt.HashPassword(body.Password);

            var newPerson = new Models.Entities.Person()
            {
                CreatedAt = DateTime.UtcNow,
                Email = body.Email,
                LastName = body.LastName,
                FirstName = body.FirstName,
                Patronymic = body.Patronymic,
                Birth = body.Birth,
                DriveLicense = body.DriveLicense,
                HashedPassword = hashedPassword,
                RoleId = body.RoleId,
                Phone = body.Phone
            };

            _context.Persons.Add(newPerson);

            await _context.SaveChangesAsync();

            return
                new PersonDto()
                {
                    PersonId = newPerson.PersonId,
                    Email = body.Email,
                    Phone = body.Phone,
                    LastName = body.LastName,
                    FirstName = body.FirstName,
                    Patronymic = body.Patronymic,
                    Birth = body.Birth,
                    DriveLicense = body.DriveLicense,
                    RoleId = body.RoleId
                };
        }
    }
}
