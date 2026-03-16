using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.EntityFrameworkCore;
using server.Contracts.Requests;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;
using System;
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

            uint? avatarId =
                    await _context.Avatars
                        .Where(a => a.PersonId == person.PersonId)
                        .OrderByDescending(a => a.CreatedAt)
                        .Select(a => a.AvatarId)
                        .FirstOrDefaultAsync();

            if (avatarId is null)
                throw new Exception();

            person.AvatarId = (uint)avatarId;

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

            DateTime createdAt = DateTime.UtcNow;

            var newPerson = new Person()
            {
                CreatedAt = createdAt,
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

            var avatar = new Avatar()
            {
                CreatedAt = createdAt,
                AvatarUrl = "standart.png",
                PersonId = newPerson.PersonId,
                ContentType = "image/png"
            };

            _context.Avatars.Add(avatar);

            await _context.SaveChangesAsync();

            var person = new PersonDto()
            {
                PersonId = newPerson.PersonId,
                Email = body.Email,
                Phone = body.Phone,
                LastName = body.LastName,
                FirstName = body.FirstName,
                Patronymic = body.Patronymic,
                Birth = body.Birth,
                DriveLicense = body.DriveLicense,
                RoleId = body.RoleId,
                AvatarId = avatar.AvatarId
            };

            return person;
        }

        public async Task UpdatePersonInfo(UpdatePersonInfoRequest body, uint personId)
        {
            var person = await _context.Persons.FirstOrDefaultAsync(p => p.PersonId == personId);

            if (person == null)
                throw new HttpError("Пользователь не найден", StatusCodes.Status404NotFound);

            
            if (string.IsNullOrWhiteSpace(body.DriveLicense))
            {
                int countOfCarsByPerson = await GetCountOfCars(personId);

                if (countOfCarsByPerson > 0)
                    throw new HttpError("Вы не можете очистить информацию о водительском удостоверении, так как у вас имеются автомобили", StatusCodes.Status409Conflict);
            }

            bool exists = await _context.Persons.AnyAsync(p => p.Email == body.Email && p.PersonId != personId);
            if (exists)
                throw new HttpError("Пользователь с данным email уже существует", StatusCodes.Status409Conflict);

            exists = await _context.Persons.AnyAsync(p => p.Phone == body.Phone && p.PersonId != personId);
            if (exists)
                throw new HttpError("Пользователь с данным номером телефона уже существует", StatusCodes.Status409Conflict);

            exists = !string.IsNullOrWhiteSpace(body.DriveLicense) && await _context.Persons.AnyAsync(p => p.DriveLicense == body.DriveLicense && p.PersonId != personId);
            if (exists)
                throw new HttpError("Пользователь с данным водительским удостоверением уже существует", StatusCodes.Status409Conflict);

            person.Email = body.Email;
            person.Phone = body.Phone;
            person.DriveLicense = body.DriveLicense;
            person.LastName = body.LastName;
            person.FirstName = body.FirstName;
            person.Patronymic = body.Patronymic;
            person.Birth = body.Birth;

            await _context.SaveChangesAsync();
        }

        private async Task<int> GetCountOfCars(uint personId)
        {
            int count = await _context.Cars
                .Where(c => c.PersonId == personId)
                .CountAsync();

            return count;
        }
    }
}
