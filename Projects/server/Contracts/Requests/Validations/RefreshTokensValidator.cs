using FluentValidation;

namespace server.Contracts.Requests.Validations
{
    public class RefreshTokensValidator : AbstractValidator<RefreshTokensRequest>
    {
        public RefreshTokensValidator()
        {
            RuleFor(x => x.RefreshToken)
                .NotEmpty().WithMessage("RefreshToken обязателен");
        }
    }
}
