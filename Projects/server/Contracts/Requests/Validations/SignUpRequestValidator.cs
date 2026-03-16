using FluentValidation;
using server.Contracts.Requests;

public class SignUpRequestValidator : AbstractValidator<SignUpRequest>
{
    public SignUpRequestValidator()
    {
        RuleFor(x => x.Email)
            .NotEmpty().WithMessage("Email обязателен")
            .EmailAddress().WithMessage("Некорректный формат email");

        RuleFor(x => x.Phone)
            .NotEmpty().WithMessage("Телефон обязателен")
            .Matches(@"^\+7\d{10}$").WithMessage("Телефон должен быть в формате +72345678901");

        RuleFor(x => x.LastName)
            .NotEmpty().WithMessage("Фамилия обязательна")
            .MaximumLength(50).WithMessage("Фамилия слишком длинная");

        RuleFor(x => x.FirstName)
            .NotEmpty().WithMessage("Имя обязательно")
            .MaximumLength(50).WithMessage("Имя слишком длинное");

        RuleFor(x => x.Patronymic)
            .MaximumLength(50).WithMessage("Отчество слишком длинное")
            .When(x => !string.IsNullOrWhiteSpace(x.Patronymic));

        RuleFor(x => x.Birth)
            .LessThan(DateOnly.FromDateTime(DateTime.Now)).WithMessage("Дата рождения должна быть в прошлом");

        RuleFor(x => x.Password)
            .NotEmpty().WithMessage("Пароль обязателен")
            .MinimumLength(8).WithMessage("Пароль должен содержать минимум 8 символов")
            .MaximumLength(32).WithMessage("Пароль может содержать максимум 32 символа");

        RuleFor(x => x.DriveLicense)
            .Matches(@"^\d{10}$").WithMessage("Некорректный формат водительских прав")
            .When(x => !string.IsNullOrEmpty(x.DriveLicense));

        RuleFor(x => x.RoleId)
            .NotEmpty().WithMessage("Роль обязательна");
    }
}