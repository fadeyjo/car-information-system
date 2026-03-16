using server.Contracts.Requests;
using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface IPersonsService
    {
        Task<PersonDto> SignUp(SignUpRequest body);
        Task<PersonDto> GetPersonById(uint personId);
    }
}
