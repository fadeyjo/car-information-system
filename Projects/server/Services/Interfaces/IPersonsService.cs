using server.Contracts.Requests;
using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface IPersonsService
    {
        Task<bool> PersonExistsById(uint personId);
        Task<bool> PersonExistsByEmail(string email);
        Task<bool> PersonExistsByPhone(string phone);
        Task<bool> PersonExistsByDriveLicense(string driveLicense);
        Task<PersonDto?> GetPersonById(uint personId);
        Task<PersonDto> SignUp(SignUpRequest body);
    }
}
