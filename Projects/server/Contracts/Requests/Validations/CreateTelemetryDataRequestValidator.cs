using FluentValidation;

namespace server.Contracts.Requests.Validations
{
    public class CreateTelemetryDataRequestValidator : AbstractValidator<CreateTelemetryDataRequest>
    {
        public CreateTelemetryDataRequestValidator()
        {
            RuleFor(x => x.RecDatetime)
                .NotEmpty().WithMessage("Дата и время записи обязательны")
                .LessThanOrEqualTo(DateTime.UtcNow);

            RuleFor(x => x.ServiceId)
                .NotEmpty().WithMessage("Сервис OBDII обязателен");

            RuleFor(x => x.PID)
                .NotEmpty().WithMessage("PID обязатален");

            RuleFor(x => x.EcuId)
                .NotEmpty().WithMessage("ID ЭБУ обязателен");

            RuleFor(x => x.ResponseDlc)
                .NotEmpty().WithMessage("Длина овтета обязательна");

            RuleFor(x => x.TripId)
                .NotEmpty().WithMessage("ID поездки обязателен");
        }
    }
}
