using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.EntityFrameworkCore;
using server.Contracts.Requests;
using server.Contracts.Responses;
using server.Data;
using server.Models.Entities;
using server.Services.Interfaces;
using server.Utils;
using System;
using System.Numerics;

namespace server.Services
{
    public class TripsService : ITripsService
    {
        private readonly AppDbContext _context;

        private readonly IOBDIIPIDSService _service;

        public TripsService(AppDbContext context, IOBDIIPIDSService service)
        {
            _context = context;
            _service = service;
        }

        public async Task DeleteTrip(ulong tripId)
        {
            var trip = await _context.Trips.FirstOrDefaultAsync(t => t.TripId == tripId);

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            _context.Trips.Remove(trip);
            await _context.SaveChangesAsync();
        }

        public async Task EndTrip(DateTime endDatetime, ulong tripId)
        {
            var trip =
                await _context.Trips.FirstOrDefaultAsync(t => t.TripId == tripId);

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            if (trip.EndDatetime != null)
                throw new HttpError("Поездка уже закончена", StatusCodes.Status409Conflict);

            if (endDatetime <= trip.StartDatetime)
                throw new HttpError(
                    "Дата и время окончания поездки должны быть позже даты и времени начала поездки",
                    StatusCodes.Status409Conflict
                );

            trip.EndDatetime = endDatetime;
            await _context.SaveChangesAsync();
        }

        public async Task<TripDto> GetTripById(ulong tripId)
        {
            var trip =
                await _context.Trips
                    .Include(t => t.Car)
                    .Include(t => t.Car.Person)
                    .Include(t => t.Car.CarConfiguration)
                    .Include(t => t.Car.CarConfiguration.CarBody)
                    .Include(t => t.Car.CarConfiguration.CarGearbox)
                    .Include(t => t.Car.CarConfiguration.CarDrive)
                    .Include(t => t.Car.CarConfiguration.CarBrandModel)
                    .Include(t => t.Car.CarConfiguration.CarBrandModel.CarBrand)
                    .Include(t => t.Car.CarConfiguration.EngineConfiguration)
                    .Include(t => t.Car.CarConfiguration.EngineConfiguration.FuelType)
                    .Where(t => t.TripId == tripId)
                    .Select(t => new TripDto()
                    {
                        TripId = t.TripId,
                        StartDatetime = t.StartDatetime,
                        DeviceId = t.DeviceId,
                        Car = new CarDto()
                        {
                            CarId = t.Car.CarId,
                            Person = new PersonDto()
                            {
                                PersonId = t.Car.Person.PersonId,
                                Email = t.Car.Person.Email,
                                Phone = t.Car.Person.Phone,
                                LastName = t.Car.Person.LastName,
                                FirstName = t.Car.Person.FirstName,
                                Patronymic = t.Car.Person.Patronymic,
                                Birth = t.Car.Person.Birth,
                                DriveLicense = t.Car.Person.DriveLicense,
                                RoleId = t.Car.Person.RoleId
                            },
                            VinNumber = t.Car.VinNumber.ToUpper(),
                            StateNumber = t.Car.StateNumber,
                            BodyName = t.Car.CarConfiguration.CarBody.BodyName,
                            ReleaseYear = t.Car.CarConfiguration.ReleaseYear,
                            GearboxName= t.Car.CarConfiguration.CarGearbox.GearboxName,
                            DriveName= t.Car.CarConfiguration.CarDrive.DriveName,
                            VehicleWeightKg= t.Car.CarConfiguration.VehicleWeightKg,
                            BrandName= t.Car.CarConfiguration.CarBrandModel.CarBrand.BrandName,
                            ModelName= t.Car.CarConfiguration.CarBrandModel.ModelName,
                            EnginePowerHp= t.Car.CarConfiguration.EngineConfiguration.EnginePowerHp,
                            EnginePowerKw = t.Car.CarConfiguration.EngineConfiguration.EnginePowerKw,
                            EngineCapacityL= t.Car.CarConfiguration.EngineConfiguration.EngineCapacityL,
                            TankCapacityL= t.Car.CarConfiguration.EngineConfiguration.TankCapacityL,
                            FuelTypeName = t.Car.CarConfiguration.EngineConfiguration.FuelType.TypeName
                        },
                        EndDatetime = t.EndDatetime
                    })
                    .FirstOrDefaultAsync();

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            var photoId = await _context.CarPhotos
                .Where(p => p.CarId == trip.Car.CarId)
                .OrderByDescending(p => p.CreatedAt)
                .Select(p => p.PhotoId)
                .FirstOrDefaultAsync();

            trip.Car.PhotoId = photoId;

            var avatarId = await _context.Avatars
                .Where(a => a.PersonId == trip.Car.Person.PersonId)
                .OrderByDescending(a => a.CreatedAt)
                .Select(a => a.AvatarId)
                .FirstOrDefaultAsync();

            trip.Car.Person.AvatarId = avatarId;

            return trip;
        }

        private byte[] GetRawResponse(uint sup)
        {
            // Со старших до младших

            var response = new byte[8];
            response[0] = 7;
            response[1] = 0x41;
            response[2] = 0;
            response[3] = (byte)(sup >> 24 & 0xFF);
            response[4] = (byte)(sup >> 16 & 0xFF);
            response[5] = (byte)(sup >> 8 & 0xFF);
            response[6] = (byte)(sup & 0xFF);
            response[7] = 0;

            return response;
        }

        public async Task<TripDto> StartTrip(DateTime startDatetime, string macAddress, uint carId, byte[] ECUId, uint supported)
        {
            macAddress = macAddress.ToUpper();

            bool exits = await _context.Cars.AnyAsync(c => c.CarId == carId);

            if (!exits)
                throw new HttpError("Автомобиль не найден", StatusCodes.Status404NotFound);

            var obdIIId = await _service.GetOBDIIPIDId(1, 0);

            if (obdIIId == null)
                throw new HttpError("PID не найден", StatusCodes.Status404NotFound);

            var device = await _context.OBDIIDevices.FirstOrDefaultAsync(d => d.MacAddress == macAddress);

            if (device == null)
            {
                device = new OBDIIDevice()
                {
                    CreatedAt = DateTime.UtcNow,
                    MacAddress = macAddress
                };
                _context.OBDIIDevices.Add(device);
                await _context.SaveChangesAsync();
            }

            var newTrip = new Trip()
            {
                StartDatetime = startDatetime,
                DeviceId = device.DeviceId,
                CarId = carId
            };
            _context.Trips.Add(newTrip);
            await _context.SaveChangesAsync();

            var supportedPidsData = new TelemetryData()
            {
                RecDatetime = startDatetime,
                OBDIIPIDId = (uint)obdIIId,
                EcuId = ECUId,
                ResponseDlc = 8,
                Response = GetRawResponse(supported),
                TripId = newTrip.TripId
            };

            _context.TelemetryData.Add(supportedPidsData);
            await _context.SaveChangesAsync();

            var trip =
                await _context.Trips
                    .Include(t => t.Car)
                    .Include(t => t.Car.Person)
                    .Include(t => t.Car.CarConfiguration)
                    .Include(t => t.Car.CarConfiguration.CarBody)
                    .Include(t => t.Car.CarConfiguration.CarGearbox)
                    .Include(t => t.Car.CarConfiguration.CarDrive)
                    .Include(t => t.Car.CarConfiguration.CarBrandModel)
                    .Include(t => t.Car.CarConfiguration.CarBrandModel.CarBrand)
                    .Include(t => t.Car.CarConfiguration.EngineConfiguration)
                    .Include(t => t.Car.CarConfiguration.EngineConfiguration.FuelType)
                    .Where(t => t.TripId == newTrip.TripId)
                    .Select(t => new TripDto()
                    {
                        TripId = t.TripId,
                        StartDatetime = t.StartDatetime,
                        DeviceId = t.DeviceId,
                        Car = new CarDto()
                        {
                            CarId = t.Car.CarId,
                            Person = new PersonDto()
                            {
                                PersonId = t.Car.Person.PersonId,
                                Email = t.Car.Person.Email,
                                Phone = t.Car.Person.Phone,
                                LastName = t.Car.Person.LastName,
                                FirstName = t.Car.Person.FirstName,
                                Patronymic = t.Car.Person.Patronymic,
                                Birth = t.Car.Person.Birth,
                                DriveLicense = t.Car.Person.DriveLicense,
                                RoleId = t.Car.Person.RoleId
                            },
                            VinNumber = t.Car.VinNumber.ToUpper(),
                            StateNumber = t.Car.StateNumber,
                            BodyName = t.Car.CarConfiguration.CarBody.BodyName,
                            ReleaseYear = t.Car.CarConfiguration.ReleaseYear,
                            GearboxName= t.Car.CarConfiguration.CarGearbox.GearboxName,
                            DriveName= t.Car.CarConfiguration.CarDrive.DriveName,
                            VehicleWeightKg= t.Car.CarConfiguration.VehicleWeightKg,
                            BrandName= t.Car.CarConfiguration.CarBrandModel.CarBrand.BrandName,
                            ModelName= t.Car.CarConfiguration.CarBrandModel.ModelName,
                            EnginePowerHp= t.Car.CarConfiguration.EngineConfiguration.EnginePowerHp,
                            EnginePowerKw = t.Car.CarConfiguration.EngineConfiguration.EnginePowerKw,
                            EngineCapacityL= t.Car.CarConfiguration.EngineConfiguration.EngineCapacityL,
                            TankCapacityL= t.Car.CarConfiguration.EngineConfiguration.TankCapacityL,
                            FuelTypeName = t.Car.CarConfiguration.EngineConfiguration.FuelType.TypeName
                        },
                        EndDatetime = t.EndDatetime
                    })
                    .FirstOrDefaultAsync();

            if (trip == null)
                throw new HttpError("Поездка не найдена", StatusCodes.Status404NotFound);

            var photoId = await _context.CarPhotos
                .Where(p => p.CarId == trip.Car.CarId)
                .OrderByDescending(p => p.CreatedAt)
                .Select(p => p.PhotoId)
                .FirstOrDefaultAsync();

            trip.Car.PhotoId = photoId;

            var avatarId = await _context.Avatars
                .Where(a => a.PersonId == trip.Car.Person.PersonId)
                .OrderByDescending(a => a.CreatedAt)
                .Select(a => a.AvatarId)
                .FirstOrDefaultAsync();

            trip.Car.Person.AvatarId = avatarId;

            return trip;
        }

        public async Task<AllTripsDto> GetAllTrips()
        {
            var id = await _context.Trips.Select(t => t.TripId).ToListAsync();

            var current = new List<TripDto>();
            var ended = new List<TripDto>();
            foreach (var tripId in id)
            {
                var trip = await GetTripById(tripId);

                if (trip.EndDatetime == null)
                {
                    current.Add(trip);
                    continue;
                }

                ended.Add(trip);
            }

            return new AllTripsDto()
            {
                current = current,
                ended = ended
            };
        }
    }
}
