using Microsoft.EntityFrameworkCore;
using server.Contracts.Requests;
using server.Contracts.Responses;
using server.Data;
using server.Services.Interfaces;

namespace server.Services
{
    public class PersonsService : IPersonsService
    {
        private readonly AppDbContext _context;
        
        public PersonsService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<PersonDto?> GetPersonById(uint personId)
        {
            return
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
        }

        public async Task<bool> PersonExistsByDriveLicense(string driveLicense)
        {
            return
                await _context.Persons.AnyAsync(p => p.DriveLicense == driveLicense);
        }

        public async Task<bool> PersonExistsByEmail(string email)
        {
            return
                await _context.Persons.AnyAsync(p => p.Email == email);
        }

        public async Task<bool> PersonExistsById(uint personId)
        {
            return
                await _context.Persons.AnyAsync(p => p.PersonId == personId);
        }

        public async Task<bool> PersonExistsByPhone(string phone)
        {
            return
                await _context.Persons.AnyAsync(p => p.Phone == phone);
        }

        public async Task<PersonDto> SignUp(SignUpRequest body)
        {
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
                RoleId = body.RoleId
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
