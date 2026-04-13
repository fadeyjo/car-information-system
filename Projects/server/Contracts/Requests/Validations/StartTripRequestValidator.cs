using FluentValidation;

namespace server.Contracts.Requests.Validations
{
    public class StartTripRequestValidator : AbstractValidator<StartTripRequest>
    {
        public StartTripRequestValidator()
        {
            RuleFor(x => x.StartDatetime)
                .NotEmpty().WithMessage("Дата и время начала обязательны")
                .LessThanOrEqualTo(DateTime.UtcNow).WithMessage("Дата и время должны быть раньше");

            RuleFor(x => x.MacAddress)
                .NotEmpty().WithMessage("MAC адрес устройства обязателен")
                .Matches(@"^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$").WithMessage("Некорректный MAC адрес");

            RuleFor(x => x.CarId)
                .NotEmpty().WithMessage("ID автомобиля обязателен");

            RuleFor(x => x.EcuId)
                .NotEmpty().WithMessage("ID ЭБУ обязателен");

            RuleFor(x => x.Supported)
                .NotEmpty().WithMessage("Ответ OBDII обязателен");
        }
    }
}
