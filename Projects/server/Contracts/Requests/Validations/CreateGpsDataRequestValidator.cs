using FluentValidation;

namespace server.Contracts.Requests.Validations
{
    public class CreateGpsDataRequestValidator : AbstractValidator<CreateGpsDataRequest>
    {
        public CreateGpsDataRequestValidator()
        {
            RuleFor(x => x.RecDateTime)
                .NotEmpty().WithMessage("Дата и время записи обязательны")
                .LessThanOrEqualTo(DateTime.UtcNow).WithMessage("Дата и время записи должны быть позднее");

            RuleFor(x => x.TripId)
                .NotEmpty().WithMessage("ID поездки обязателен");

            RuleFor(x => x.LatitudeDeg)
                .NotEmpty().WithMessage("Широта обязательна")
                .GreaterThanOrEqualTo(-90).WithMessage("Долгота должна быть больше -90 градусов")
                .LessThanOrEqualTo(90).WithMessage("Долгота должна быть меньше или равна 90 градусов");

            RuleFor(x => x.LongitudeDeg)
                .NotEmpty().WithMessage("Долгота обязательна")
                .GreaterThanOrEqualTo(-180).WithMessage("Широта должна быть больше или равна -180 градусов")
                .LessThanOrEqualTo(180).WithMessage("Широта должна быть меньше или равна 180 градусов"); ;

            RuleFor(x => x.BearingDeg)
                .InclusiveBetween(0, 360)
                .When(x => x.BearingDeg.HasValue)
                .WithMessage("Курс должен быть в диапазоне 0–360");
        }
    }
}
