using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.EntityFrameworkCore;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;
using System.Numerics;

namespace server.Services
{
    public class CarsService : ICarsService
    {
        private readonly AppDbContext _context;

        public CarsService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<CarDto> CreateCar(
            string vinNumber, string? stateNumber,
            string brandName, string modelName,
            string bodyName, ushort releaseYear,
            string gearboxName, string driveName,
            ushort vehicleWeightKg, ushort enginePowerHp,
            float enginePowerKw, float engineCapacityL,
            byte tankCapacityL, string fuelTypeName,
            uint personId
        )
        {
            var person = await _context.Persons.FirstOrDefaultAsync(c => c.PersonId == personId);

            if (person == null)
                throw new HttpError("Пользователь не найден", StatusCodes.Status404NotFound);

            if (string.IsNullOrWhiteSpace(person.DriveLicense))
                throw new HttpError("Регистрация автомобиля невозможна, так как у вас отсутствует водительское удостоверение", StatusCodes.Status409Conflict);

            bool exists = await _context.Cars.AnyAsync(c => c.VinNumber == vinNumber.ToUpper());

            if (exists)
                throw new HttpError("Автомобиль с данным VIN уже существует", StatusCodes.Status409Conflict);

            if (!string.IsNullOrWhiteSpace(stateNumber))
            {
                exists = await _context.Cars.AnyAsync(c => c.StateNumber == stateNumber.ToUpper());
                if (exists)
                    throw new HttpError("Автомобиль с данным гос. номером уже существует", StatusCodes.Status409Conflict);
            }

            var fuelType =
                await _context.FuelTypes
                    .Where(ft => ft.TypeName == fuelTypeName)
                    .Select(ft => new { ft.TypeId })
                    .FirstOrDefaultAsync();
            if (fuelType is null)
                throw new HttpError("Неизвестный тип топлива", StatusCodes.Status404NotFound);

            uint? engineConfigurationId = await GetEngineConfigurationId(
                enginePowerHp, enginePowerKw,
                engineCapacityL, tankCapacityL,
                fuelType.TypeId
            );

            if (engineConfigurationId == null)
            {
                var newEngineConfiguration = new EngineConfiguration()
                {
                    EnginePowerHp = enginePowerHp,
                    EnginePowerKw = enginePowerKw,
                    EngineCapacityL = engineCapacityL,
                    TankCapacityL = tankCapacityL,
                    FuelTypeId = fuelType.TypeId
                };

                _context.EngineConfigurations.Add(newEngineConfiguration);

                await _context.SaveChangesAsync();

                engineConfigurationId = newEngineConfiguration.EngineConfigId;
            }

            if (engineConfigurationId is null)
                throw new Exception();

            var carBrand =
                await _context.CarBrands
                    .Where(cb => cb.BrandName == brandName)
                    .Select(cb => new { cb.BrandId })
                    .FirstOrDefaultAsync();
            if (carBrand == null)
                throw new HttpError("Неизвестный брэнд автомобиля", StatusCodes.Status404NotFound);

            var carBrandModel =
                await _context.CarBrandsModels
                    .Where(cbm => cbm.BrandId == carBrand.BrandId && cbm.ModelName == modelName)
                    .Select(cbm => new { cbm.CarBrandModelId })
                    .FirstOrDefaultAsync();
            if (carBrandModel == null)
                throw new HttpError("Неизвестная модель автомобиля", StatusCodes.Status404NotFound);

            var carBody =
                await _context.CarBodies
                    .Where(cb => cb.BodyName == bodyName)
                    .Select(cbm => new { cbm.BodyId })
                    .FirstOrDefaultAsync();
            if (carBody == null)
                throw new HttpError("Неизвестный кузов автомобиля", StatusCodes.Status404NotFound);

            var carGearbox =
                await _context.CarGearboxes
                    .Where(cg => cg.GearboxName == gearboxName)
                    .Select(cg => new { cg.GearboxId })
                    .FirstOrDefaultAsync();
            if (carGearbox is null)
                throw new HttpError("Неизвестный тип КПП автомобиля", StatusCodes.Status404NotFound);

            var carDrive =
                await _context.CarDrives
                    .Where(cd => cd.DriveName == driveName)
                    .Select(cd => new { cd.DriveId })
                    .FirstOrDefaultAsync();
            if (carDrive == null)
                throw new HttpError("Неизвестный тип привода автомобиля", StatusCodes.Status404NotFound);

            uint? carConfigurationId = await GetCarConfigurationId(
                carBrandModel.CarBrandModelId, carBody.BodyId,
                releaseYear, carGearbox.GearboxId,
                carDrive.DriveId, (uint)engineConfigurationId,
                vehicleWeightKg
            );

            if (carConfigurationId == null)
            {
                var newCarConfiguration = new CarConfiguration()
                {
                    CarBrandModelId = carBrandModel.CarBrandModelId,
                    BodyId = carBody.BodyId,
                    ReleaseYear = releaseYear,
                    GearboxId = carGearbox.GearboxId,
                    DriveId = carDrive.DriveId,
                    EngineConfId = (uint)engineConfigurationId,
                    VehicleWeightKg = vehicleWeightKg
                };

                _context.CarConfigurations.Add(newCarConfiguration);

                await _context.SaveChangesAsync();

                carConfigurationId = newCarConfiguration.CarConfigId;
            }

            if (carConfigurationId == null)
                throw new Exception();

            var createdAt = DateTime.UtcNow;

            Car newCar = new()
            {
                PersonId = personId,
                CreatedAt = createdAt,
                VinNumber = vinNumber.ToUpper(),
                StateNumber = stateNumber?.ToUpper(),
                CarConfigId = (uint)carConfigurationId
            };

            _context.Cars.Add(newCar);

            await _context.SaveChangesAsync();

            var carPhoto = new CarPhoto()
            {
                CreatedAt = createdAt,
                PhotoUrl = "standart.png",
                CarId = newCar.CarId,
                ContentType = "image/png"
            };

            _context.CarPhotos.Add(carPhoto);

            await _context.SaveChangesAsync();

            var car = new CarDto()
            {
                CarId = newCar.CarId,
                PersonId = personId,
                VinNumber = vinNumber.ToUpper(),
                StateNumber= stateNumber?.ToUpper(),
                BodyName = bodyName,
                ReleaseYear = releaseYear,
                GearboxName= gearboxName,
                DriveName= driveName,
                VehicleWeightKg= vehicleWeightKg,
                BrandName= brandName,
                ModelName= modelName,
                EnginePowerHp= enginePowerHp,
                EnginePowerKw = enginePowerKw,
                EngineCapacityL= engineCapacityL,
                TankCapacityL= tankCapacityL,
                FuelTypeName = fuelTypeName,
                PhotoId = carPhoto.PhotoId
            };

            return car;
        }

        public async Task<CarDto> GetCarById(uint carId)
        {
            var car =
                await _context.Cars
                    .Where(c => c.CarId == carId)
                    .Select(c => new CarDto
                    {
                        CarId = c.CarId,
                        VinNumber = c.VinNumber,
                        StateNumber = c.StateNumber,
                        BrandName = c.CarConfiguration.CarBrandModel.CarBrand.BrandName,
                        ModelName = c.CarConfiguration.CarBrandModel.ModelName,
                        BodyName = c.CarConfiguration.CarBody.BodyName,
                        ReleaseYear = c.CarConfiguration.ReleaseYear,
                        GearboxName = c.CarConfiguration.CarGearbox.GearboxName,
                        DriveName = c.CarConfiguration.CarDrive.DriveName,
                        VehicleWeightKg = c.CarConfiguration.VehicleWeightKg,
                        EnginePowerHp = c.CarConfiguration.EngineConfiguration.EnginePowerHp,
                        EnginePowerKw = c.CarConfiguration.EngineConfiguration.EnginePowerKw,
                        EngineCapacityL = c.CarConfiguration.EngineConfiguration.EngineCapacityL,
                        TankCapacityL = c.CarConfiguration.EngineConfiguration.TankCapacityL,
                        FuelTypeName = c.CarConfiguration.EngineConfiguration.FuelType.TypeName,
                        PersonId = c.PersonId
                    })
                    .FirstOrDefaultAsync();

            if (car == null)
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);

            uint? photoId =
                await _context.CarPhotos
                    .Where(c => c.CarId == car.CarId)
                    .OrderByDescending(c => c.CreatedAt)
                    .Select(c => c.PhotoId)
                    .FirstOrDefaultAsync();

            if (photoId is null)
                throw new Exception();

            car.PhotoId = (uint)photoId;

            return car;
        }

        public async Task<List<CarDto>> GetCarsByPersonId(uint personId)
        {
            bool exists =
                await _context.Persons.AnyAsync(p => p.PersonId == personId);

            if (!exists)
                throw new HttpError("Пользователь не найден", StatusCodes.Status404NotFound);

            var cars =
                await _context.Cars
                    .Where(c => c.PersonId == personId)
                    .Select(c => new CarDto
                    {
                        CarId = c.CarId,
                        VinNumber = c.VinNumber,
                        StateNumber = c.StateNumber,
                        BrandName = c.CarConfiguration.CarBrandModel.CarBrand.BrandName,
                        ModelName = c.CarConfiguration.CarBrandModel.ModelName,
                        BodyName = c.CarConfiguration.CarBody.BodyName,
                        ReleaseYear = c.CarConfiguration.ReleaseYear,
                        GearboxName = c.CarConfiguration.CarGearbox.GearboxName,
                        DriveName = c.CarConfiguration.CarDrive.DriveName,
                        VehicleWeightKg = c.CarConfiguration.VehicleWeightKg,
                        EnginePowerHp = c.CarConfiguration.EngineConfiguration.EnginePowerHp,
                        EnginePowerKw = c.CarConfiguration.EngineConfiguration.EnginePowerKw,
                        EngineCapacityL = c.CarConfiguration.EngineConfiguration.EngineCapacityL,
                        TankCapacityL = c.CarConfiguration.EngineConfiguration.TankCapacityL,
                        FuelTypeName = c.CarConfiguration.EngineConfiguration.FuelType.TypeName,
                        PersonId = c.PersonId
                    })
                    .ToListAsync();

            for (int i = 0; i < cars.Count; i++)
            {
                uint? photoId =
                await _context.CarPhotos
                    .Where(c => c.CarId == cars[i].CarId)
                    .OrderByDescending(c => c.CreatedAt)
                    .Select(c => c.PhotoId)
                    .FirstOrDefaultAsync();

                if (photoId is null)
                    throw new Exception();

                cars[i].PhotoId = (uint)photoId;
            }

            return cars;
        }

        public async Task UpdateCarInfo(
            string vinNumber, string? stateNumber,
            string brandName, string modelName,
            string bodyName, ushort releaseYear,
            string gearboxName, string driveName,
            ushort vehicleWeightKg, ushort enginePowerHp,
            float enginePowerKw, float engineCapacityL,
            byte tankCapacityL, string fuelTypeName,
            uint carId
        )
        {
            var carToUpdate = await _context.Cars.FirstOrDefaultAsync(c => c.CarId == carId);

            if (carToUpdate == null)
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);

            bool exists = await _context.Cars.AnyAsync(c => c.VinNumber == vinNumber.ToUpper() && c.CarId != carId);

            if (exists)
                throw new HttpError("Автомобиль с данным VIN уже существует", StatusCodes.Status409Conflict);

            if (!string.IsNullOrWhiteSpace(stateNumber))
            {
                exists = await _context.Cars.AnyAsync(c => c.StateNumber == stateNumber.ToUpper() && c.CarId != carId);
                if (exists)
                    throw new HttpError("Автомобиль с данным гос. номером уже существует", StatusCodes.Status409Conflict);
            }

            var fuelType =
                await _context.FuelTypes
                    .Where(ft => ft.TypeName == fuelTypeName)
                    .Select(ft => new { ft.TypeId })
                    .FirstOrDefaultAsync();
            if (fuelType is null)
                throw new HttpError("Неизвестный тип топлива", StatusCodes.Status404NotFound);

            uint? engineConfigurationId = await GetEngineConfigurationId(
                enginePowerHp, enginePowerKw,
                engineCapacityL, tankCapacityL,
                fuelType.TypeId
            );

            if (engineConfigurationId == null)
            {
                var newEngineConfiguration = new EngineConfiguration()
                {
                    EnginePowerHp = enginePowerHp,
                    EnginePowerKw = enginePowerKw,
                    EngineCapacityL = engineCapacityL,
                    TankCapacityL = tankCapacityL,
                    FuelTypeId = fuelType.TypeId
                };

                _context.EngineConfigurations.Add(newEngineConfiguration);

                await _context.SaveChangesAsync();

                engineConfigurationId = newEngineConfiguration.EngineConfigId;
            }

            if (engineConfigurationId is null)
                throw new Exception();

            var carBrand =
                await _context.CarBrands
                    .Where(cb => cb.BrandName == brandName)
                    .Select(cb => new { cb.BrandId })
                    .FirstOrDefaultAsync();
            if (carBrand == null)
                throw new HttpError("Неизвестный брэнд автомобиля", StatusCodes.Status404NotFound);

            var carBrandModel =
                await _context.CarBrandsModels
                    .Where(cbm => cbm.BrandId == carBrand.BrandId && cbm.ModelName == modelName)
                    .Select(cbm => new { cbm.CarBrandModelId })
                    .FirstOrDefaultAsync();
            if (carBrandModel == null)
                throw new HttpError("Неизвестная модель автомобиля", StatusCodes.Status404NotFound);

            var carBody =
                await _context.CarBodies
                    .Where(cb => cb.BodyName == bodyName)
                    .Select(cbm => new { cbm.BodyId })
                    .FirstOrDefaultAsync();
            if (carBody == null)
                throw new HttpError("Неизвестный кузов автомобиля", StatusCodes.Status404NotFound);

            var carGearbox =
                await _context.CarGearboxes
                    .Where(cg => cg.GearboxName == gearboxName)
                    .Select(cg => new { cg.GearboxId })
                    .FirstOrDefaultAsync();
            if (carGearbox is null)
                throw new HttpError("Неизвестный тип КПП автомобиля", StatusCodes.Status404NotFound);

            var carDrive =
                await _context.CarDrives
                    .Where(cd => cd.DriveName == driveName)
                    .Select(cd => new { cd.DriveId })
                    .FirstOrDefaultAsync();
            if (carDrive == null)
                throw new HttpError("Неизвестный тип привода автомобиля", StatusCodes.Status404NotFound);

            uint? carConfigurationId = await GetCarConfigurationId(
                carBrandModel.CarBrandModelId, carBody.BodyId,
                releaseYear, carGearbox.GearboxId,
                carDrive.DriveId, (uint)engineConfigurationId,
                vehicleWeightKg
            );

            if (carConfigurationId == null)
            {
                var newCarConfiguration = new CarConfiguration()
                {
                    CarBrandModelId = carBrandModel.CarBrandModelId,
                    BodyId = carBody.BodyId,
                    ReleaseYear = releaseYear,
                    GearboxId = carGearbox.GearboxId,
                    DriveId = carDrive.DriveId,
                    EngineConfId = (uint)engineConfigurationId,
                    VehicleWeightKg = vehicleWeightKg
                };

                _context.CarConfigurations.Add(newCarConfiguration);

                await _context.SaveChangesAsync();

                carConfigurationId = newCarConfiguration.CarConfigId;
            }

            if (carConfigurationId == null)
                throw new Exception();

            carToUpdate.VinNumber = vinNumber.ToUpper();
            carToUpdate.StateNumber = stateNumber?.ToUpper();
            carToUpdate.CarConfigId = (uint)carConfigurationId;

            await _context.SaveChangesAsync();
        }

        private async Task<uint?> GetEngineConfigurationId(
            ushort enginePowerHP, float enginePowerKW,
            float engineCapacityL, byte tankCapacityL,
            byte fuelTypeId
        )
        {
            var engineConfiguration =
                await _context.EngineConfigurations
                    .Where(ec =>
                        ec.EnginePowerHp == enginePowerHP &&
                        ec.EnginePowerKw == enginePowerKW &&
                        ec.EngineCapacityL == engineCapacityL &&
                        ec.TankCapacityL == tankCapacityL &&
                        ec.FuelTypeId == fuelTypeId
                    )
                    .Select(ec => new { ec.EngineConfigId })
                    .FirstOrDefaultAsync();

            if (engineConfiguration is null)
                return null;

            return engineConfiguration.EngineConfigId;
        }

        private async Task<uint?> GetCarConfigurationId(
            uint carBrandModelId, byte bodyId,
            ushort releaseYear, byte gearboxId,
            byte driveId, uint engineConfId,
            ushort vehicleWeightKG
        )
        {
            var carConfiguration =
            await _context.CarConfigurations
                .Where(cc =>
                    cc.CarBrandModelId == carBrandModelId &&
                    cc.BodyId == bodyId &&
                    cc.ReleaseYear == releaseYear &&
                    cc.GearboxId == gearboxId &&
                    cc.DriveId == driveId &&
                    cc.EngineConfId == engineConfId &&
                    cc.VehicleWeightKg == vehicleWeightKG
                )
                .Select(cc => new { cc.CarConfigId })
                .FirstOrDefaultAsync();

            if (carConfiguration is null)
                return null;

            return carConfiguration.CarConfigId;
        }

        public async Task DeleteCar(uint carId)
        {
            var car = await _context.Cars.Where(c => c.CarId == carId).FirstOrDefaultAsync();

            if (car == null)
            {
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);
            }

            _context.Cars.Remove(car);

            await _context.SaveChangesAsync();
        }
    }
}
