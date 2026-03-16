using FluentValidation;
using FluentValidation.Results;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace server.Filters
{
    public class ValidationFilter : IAsyncActionFilter
    {
        private readonly IServiceProvider _serviceProvider;

        public ValidationFilter(IServiceProvider serviceProvider)
        {
            _serviceProvider = serviceProvider;
        }

        public async Task OnActionExecutionAsync(
            ActionExecutingContext context,
            ActionExecutionDelegate next
        )
        {
            foreach (var argument in context.ActionArguments.Values)
            {
                if (argument == null)
                    continue;

                var validatorType = typeof(IValidator<>).MakeGenericType(argument.GetType());
                var validator = _serviceProvider.GetService(validatorType);

                if (validator == null)
                    continue;

                var method = validatorType.GetMethod("ValidateAsync", new[] { argument.GetType(), typeof(CancellationToken) });

                if (method == null)
                    continue;

                var task = (Task)method.Invoke(validator, new object[] { argument, CancellationToken.None })!;
                await task;

                var result = (ValidationResult)task
                    .GetType()
                    .GetProperty("Result")!
                    .GetValue(task)!;

                if (!result.IsValid)
                {
                    context.Result = new BadRequestObjectResult(result.Errors);
                    return;
                }
            }

            await next();
        }
    }
}
