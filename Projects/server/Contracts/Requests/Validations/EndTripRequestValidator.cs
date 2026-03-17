using FluentValidation;

namespace server.Contracts.Requests.Validations
{
    public class EndTripRequestValidator : AbstractValidator<EndTripRequest>
    {
        public EndTripRequestValidator()
        {
            RuleFor(x => x.TripId)
                .NotEmpty().WithMessage("ID поездки обязателен");

            RuleFor(x => x.EndDatetime)
                .NotEmpty().WithMessage("Дата и время окончания поездки обязательны")
                .LessThanOrEqualTo(DateTime.UtcNow);
        }
    }
}
