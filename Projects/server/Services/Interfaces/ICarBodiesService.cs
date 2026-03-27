using server.Contracts.Responses;

namespace server.Services.Interfaces
{
    public interface ICarBodiesService
    {
        Task<List<CarBodyDto>> GetAllCarBodies();
    }
}
