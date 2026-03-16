using FluentValidation;
using Microsoft.EntityFrameworkCore;
using server.Contracts.Requests;
using server.Data;
using server.Services;
using server.Services.Interfaces;

namespace server.Program
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);

            var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");

            if (string.IsNullOrWhiteSpace(connectionString))
            {
                Console.WriteLine("Не удалось найти ConnectionStrings[DefaultConnection] в appsettings.json");
                return;
            }

            // Add services to the container.
            builder.Services.AddDbContext<AppDbContext>(options =>
                options.UseMySql(connectionString, ServerVersion.AutoDetect(connectionString))
            );

            // Services for interfaces for services
            builder.Services.AddScoped<IPersonsService, PersonsService>();

            builder.Services.AddControllers();

            // Services for validators
            builder.Services.AddValidatorsFromAssemblyContaining<SignUpRequestValidator>();

            // Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
            builder.Services.AddEndpointsApiExplorer();
            builder.Services.AddSwaggerGen();


            var app = builder.Build();

            // Configure the HTTP request pipeline.
            if (app.Environment.IsDevelopment())
            {
                app.UseSwagger();
                app.UseSwaggerUI();
            }

            app.UseHttpsRedirection();

            app.UseAuthorization();


            app.MapControllers();

            app.Run();
        }
    }
}
