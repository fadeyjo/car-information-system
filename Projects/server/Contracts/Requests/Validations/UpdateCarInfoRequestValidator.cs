using FluentValidation;

namespace server.Contracts.Requests.Validations
{
    public class UpdateCarInfoRequestValidator : AbstractValidator<UpdateCarInfoRequest>
    {
        public UpdateCarInfoRequestValidator()
        {
            RuleFor(x => x.VinNumber)
                .NotEmpty().WithMessage("VIN обязателен")
                .Matches(@"^[A-Za-z0-9]{17}$").WithMessage("Некорректный формат VIN");

            RuleFor(x => x.StateNumber)
                .Matches(@"^[АВЕКМНОРСТУХ][0-9]{3}[АВЕКМНОРСТУХ]{2}[0-9]{2,3}$").WithMessage("Некорректный формат гос. номера")
                .When(x => !string.IsNullOrEmpty(x.StateNumber));

            RuleFor(x => x.BrandName)
                .NotEmpty().WithMessage("Брэнд автомобиля обязателен");

            RuleFor(x => x.ModelName)
                .NotEmpty().WithMessage("Модель автомобиля обязательна");

            RuleFor(x => x.BodyName)
                .NotEmpty().WithMessage("Кузов автомобиля обязателен");

            RuleFor(x => x.ReleaseYear)
                .NotEmpty().WithMessage("Год выпуска автомобиля обязателен")
                .GreaterThanOrEqualTo((ushort)2000)
                .WithMessage("Можно регистрировать автомобиля, выпущенные не раньше 2000 года")
                .LessThanOrEqualTo((ushort)DateTime.Now.Year)
                .WithMessage("Год выпуска не может быть больше текущего");

            RuleFor(x => x.GearboxName)
                .NotEmpty().WithMessage("Тип КПП автомобиля обязателен");

            RuleFor(x => x.DriveName)
                .NotEmpty().WithMessage("Тип привода автомобиля обязателен");

            RuleFor(x => x.VehicleWeightKg)
                .NotEmpty().WithMessage("Масса автомобиля обязательна")
                .GreaterThanOrEqualTo((ushort)750).WithMessage("Масса автомобиля не может быть меньше 750 кг")
                .LessThanOrEqualTo((ushort)5000).WithMessage("Масса автомобиля не может быть больше 5 т");

            RuleFor(x => x.EnginePowerHp)
                .NotEmpty().WithMessage("Мощность двигателя (л.с.) обязательна")
                .GreaterThanOrEqualTo((ushort)60).WithMessage("Мощность двигателя не может быть меньше 60 л.с.")
                .LessThanOrEqualTo((ushort)5000).WithMessage("Мощность двигателя не может быть больше 5000 л.с.");

            RuleFor(x => x.EnginePowerKw)
                .NotEmpty().WithMessage("Мощность двигателя (кВт) обязательна")
                .GreaterThanOrEqualTo(2).WithMessage("Мощность двигателя не может быть меньше 2 кВт")
                .LessThanOrEqualTo(2000).WithMessage("Мощность двигателя не может быть больше 2000 кВт");

            RuleFor(x => x.EngineCapacityL)
                .NotEmpty().WithMessage("Объём двигателя (л) обязателен")
                .GreaterThanOrEqualTo((float)0.4).WithMessage("Объём двигателя не может быть меньше 0,4 л")
                .LessThanOrEqualTo(50).WithMessage("Объём двигателя не может быть больше 50 л");

            RuleFor(x => x.TankCapacityL)
                .NotEmpty().WithMessage("Объём бака (л) обязателен")
                .GreaterThanOrEqualTo((byte)10).WithMessage("Объём бака не может быть меньше 10 л")
                .LessThanOrEqualTo((byte)250).WithMessage("Объём бака не может быть больше 250 л");

            RuleFor(x => x.FuelTypeName)
                .NotEmpty().WithMessage("Тип топлива обязателен");
        }
    }
}
