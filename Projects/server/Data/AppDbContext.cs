using Microsoft.EntityFrameworkCore;
using server.Models.Entities;

namespace server.Data
{
    public class AppDbContext(DbContextOptions options) : DbContext(options)
    {
        public DbSet<Role> Roles => Set<Role>();
        public DbSet<Person> Persons => Set<Person>();
        public DbSet<CarBody> CarBodies => Set<CarBody>();
        public DbSet<CarGearbox> CarGearboxes => Set<CarGearbox>();
        public DbSet<FuelType> FuelTypes => Set<FuelType>();
        public DbSet<CarDrive> CarDrives => Set<CarDrive>();
        public DbSet<EngineConfiguration> EngineConfigurations => Set<EngineConfiguration>();
        public DbSet<CarBrand> CarBrands => Set<CarBrand>();
        public DbSet<CarBrandModel> CarBrandsModels => Set<CarBrandModel>();
        public DbSet<CarConfiguration> CarConfigurations => Set<CarConfiguration>();
        public DbSet<Car> Cars => Set<Car>();
        public DbSet<OBDIIService> OBDIIServices => Set<OBDIIService>();
        public DbSet<OBDIIPID> OBDIIPIDs => Set<OBDIIPID>();
        public DbSet<OBDIIDevice> OBDIIDevices => Set<OBDIIDevice>();
        public DbSet<Trip> Trips => Set<Trip>();
        public DbSet<TelemetryData> TelemetryData => Set<TelemetryData>();
        public DbSet<GPSData> GPSData => Set<GPSData>();
        public DbSet<RefreshToken> RefreshTokens => Set<RefreshToken>();
        public DbSet<Avatar> Avatars => Set<Avatar>();
        public DbSet<CarPhoto> CarPhotos => Set<CarPhoto>();

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            modelBuilder.Entity<Role>(entity =>
            {
                entity.ToTable("roles");

                entity.Property(r => r.RoleId).HasColumnName("role_id");

                entity.Property(r => r.RoleName).HasColumnName("role_name");

                entity.HasKey(r => r.RoleId);

                entity.HasIndex(r => r.RoleName).IsUnique();
            });

            modelBuilder.Entity<Person>(entity =>
            {
                entity.ToTable("persons");

                entity.Property(p => p.PersonId).HasColumnName("person_id");

                entity.Property(p => p.CreatedAt).HasColumnName("created_at");

                entity.Property(p => p.Email).HasColumnName("email");

                entity.Property(p => p.Phone).HasColumnName("phone");

                entity.Property(p => p.LastName).HasColumnName("last_name");

                entity.Property(p => p.FirstName).HasColumnName("first_name");

                entity.Property(p => p.Patronymic).HasColumnName("patronymic");

                entity.Property(p => p.Birth).HasColumnName("birth");

                entity.Property(p => p.HashedPassword).HasColumnName("hashed_password");

                entity.Property(p => p.DriveLicense).HasColumnName("drive_liсense");

                entity.Property(p => p.RoleId).HasColumnName("role_id");

                entity.HasKey(p => p.PersonId);

                entity.HasIndex(p => p.Email).IsUnique();

                entity.HasIndex(p => p.Phone).IsUnique();

                entity.HasIndex(p => p.DriveLicense).IsUnique();

                entity.HasOne(p => p.Role).WithMany().HasForeignKey(p => p.RoleId).OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<CarBody>(entity =>
            {
                entity.ToTable("car_bodies");

                entity.Property(cb => cb.BodyId).HasColumnName("body_id");

                entity.Property(cb => cb.BodyName).HasColumnName("body_name");

                entity.HasKey(cb => cb.BodyId);

                entity.HasIndex(cb => cb.BodyName).IsUnique();
            });

            modelBuilder.Entity<CarGearbox>(entity =>
            {
                entity.ToTable("car_gearboxes");

                entity.Property(cg => cg.GearboxId).HasColumnName("gearbox_id");

                entity.Property(cg => cg.GearboxName).HasColumnName("gearbox_name");

                entity.HasKey(cg => cg.GearboxId);

                entity.HasIndex(cg => cg.GearboxName).IsUnique();
            });

            modelBuilder.Entity<FuelType>(entity =>
            {
                entity.ToTable("fuel_types");

                entity.Property(ft => ft.TypeId).HasColumnName("type_id");

                entity.Property(ft => ft.TypeName).HasColumnName("type_name");

                entity.HasKey(ft => ft.TypeId);

                entity.HasIndex(ft => ft.TypeName).IsUnique();
            });

            modelBuilder.Entity<CarDrive>(entity =>
            {
                entity.ToTable("car_drives");

                entity.Property(cd => cd.DriveId).HasColumnName("drive_id");

                entity.Property(cd => cd.DriveName).HasColumnName("drive_name");

                entity.HasKey(cd => cd.DriveId);

                entity.HasIndex(cd => cd.DriveName).IsUnique();
            });

            modelBuilder.Entity<EngineConfiguration>(entity =>
            {
                entity.ToTable("engine_configurations");

                entity.Property(ec => ec.EngineConfigId).HasColumnName("engine_config_id");

                entity.Property(ec => ec.EnginePowerHp).HasColumnName("engine_power_hp");

                entity.Property(ec => ec.EnginePowerKw).HasColumnName("engine_power_kW");

                entity.Property(ec => ec.EngineCapacityL).HasColumnName("engine_capacity_l");

                entity.Property(ec => ec.TankCapacityL).HasColumnName("tank_capacity_l");

                entity.Property(ec => ec.FuelTypeId).HasColumnName("fuel_type_id");

                entity.HasKey(ec => ec.EngineConfigId);

                entity.HasOne(ec => ec.FuelType).WithMany().HasForeignKey(ec => ec.FuelTypeId).OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(ec => new {
                    ec.EnginePowerHp,
                    ec.EnginePowerKw,
                    ec.EngineCapacityL,
                    ec.TankCapacityL,
                    ec.FuelTypeId
                }).IsUnique();
            });

            modelBuilder.Entity<CarBrand>(entity =>
            {
                entity.ToTable("car_brands");

                entity.Property(cr => cr.BrandId).HasColumnName("brand_id");

                entity.Property(cr => cr.BrandName).HasColumnName("brand_name");

                entity.HasKey(cb => cb.BrandId);

                entity.HasIndex(cb => cb.BrandName).IsUnique();
            });

            modelBuilder.Entity<CarBrandModel>(entity =>
            {
                entity.ToTable("car_brands_models");

                entity.Property(cbm => cbm.CarBrandModelId).HasColumnName("car_brand_model_id");

                entity.Property(cbm => cbm.ModelName).HasColumnName("model_name");

                entity.Property(cbm => cbm.BrandId).HasColumnName("brand_id");

                entity.HasKey(cbm => cbm.CarBrandModelId);

                entity.HasIndex(cbm => new {
                    cbm.ModelName,
                    cbm.BrandId
                }).IsUnique();

                entity.HasOne(cbm => cbm.CarBrand).WithMany().HasForeignKey(cbm => cbm.BrandId).OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<CarConfiguration>(entity =>
            {
                entity.ToTable("car_configurations");

                entity.Property(cc => cc.CarConfigId).HasColumnName("car_config_id");

                entity.Property(cc => cc.CarBrandModelId).HasColumnName("car_brand_model_id");

                entity.Property(cc => cc.BodyId).HasColumnName("body_id");

                entity.Property(cc => cc.ReleaseYear).HasColumnName("release_year");

                entity.Property(cc => cc.GearboxId).HasColumnName("gearbox_id");

                entity.Property(cc => cc.DriveId).HasColumnName("drive_id");

                entity.Property(cc => cc.EngineConfId).HasColumnName("engine_conf_id");

                entity.Property(cc => cc.VehicleWeightKg).HasColumnName("vehicle_weight_kg");

                entity.HasKey(cc => cc.CarConfigId);

                entity.HasOne(cc => cc.CarBrandModel).WithMany().HasForeignKey(cc => cc.CarBrandModelId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(cc => cc.CarBody).WithMany().HasForeignKey(cc => cc.BodyId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(cc => cc.CarGearbox).WithMany().HasForeignKey(cc => cc.GearboxId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(cc => cc.CarDrive).WithMany().HasForeignKey(cc => cc.DriveId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(cc => cc.EngineConfiguration).WithMany().HasForeignKey(cc => cc.EngineConfId).OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(cc => new {
                    cc.CarBrandModelId,
                    cc.BodyId,
                    cc.ReleaseYear,
                    cc.GearboxId,
                    cc.DriveId,
                    cc.EngineConfId,
                    cc.VehicleWeightKg
                }).IsUnique();
            });

            modelBuilder.Entity<Car>(entity =>
            {
                entity.ToTable("cars");

                entity.Property(c => c.CarId).HasColumnName("car_id");

                entity.Property(c => c.CreatedAt).HasColumnName("created_at");

                entity.Property(c => c.PersonId).HasColumnName("person_id");

                entity.Property(c => c.VinNumber).HasColumnName("VIN_number");

                entity.Property(c => c.StateNumber).HasColumnName("state_number");

                entity.Property(c => c.CarConfigId).HasColumnName("car_config_id");

                entity.HasKey(c => c.CarId);

                entity.HasOne(c => c.Person).WithMany().HasForeignKey(c => c.PersonId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(c => c.CarConfiguration).WithMany().HasForeignKey(c => c.CarConfigId).OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(c => c.VinNumber).IsUnique();

                entity.HasIndex(c => c.StateNumber).IsUnique();
            });

            modelBuilder.Entity<OBDIIService>(entity =>
            {
                entity.ToTable("OBDII_services");

                entity.Property(s => s.ServiceId).HasColumnName("service_id");

                entity.Property(s => s.ServiceDescription).HasColumnName("service_description");

                entity.HasKey(s => s.ServiceId);
            });

            modelBuilder.Entity<OBDIIPID>(entity =>
            {
                entity.ToTable("OBDII_PIDs");

                entity.Property(p => p.OBDIIPIDId).HasColumnName("OBDII_PID_id");

                entity.Property(p => p.ServiceId).HasColumnName("service_id");

                entity.Property(p => p.PID).HasColumnName("PID");

                entity.Property(p => p.PIDDescription).HasColumnName("PID_description");

                entity.Property(p => p.Once).HasColumnName("once");

                entity.HasKey(p => p.OBDIIPIDId);

                entity.HasOne(p => p.OBDIIService).WithMany().HasForeignKey(p => p.ServiceId).OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(p => new
                {
                    p.ServiceId,
                    p.PID
                }).IsUnique();
            });

            modelBuilder.Entity<OBDIIDevice>(entity =>
            {
                entity.ToTable("OBDII_devices");

                entity.Property(d => d.DeviceId).HasColumnName("device_id");

                entity.Property(d => d.MacAddress).HasColumnName("MAC_address");

                entity.Property(d => d.CreatedAt).HasColumnName("created_at");

                entity.HasKey(d => d.DeviceId);

                entity.HasIndex(d => d.MacAddress).IsUnique();
            });

            modelBuilder.Entity<Trip>(entity =>
            {
                entity.ToTable("trips");

                entity.Property(t => t.TripId).HasColumnName("trip_id");

                entity.Property(t => t.StartDatetime).HasColumnName("start_datetime");

                entity.Property(t => t.DeviceId).HasColumnName("device_id");

                entity.Property(t => t.CarId).HasColumnName("car_id");

                entity.Property(t => t.EndDatetime).HasColumnName("end_datetime");

                entity.HasKey(t => t.TripId);

                entity.HasOne(t => t.OBDIIDevice).WithMany().HasForeignKey(t => t.DeviceId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(t => t.Car).WithMany().HasForeignKey(t => t.CarId).OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<TelemetryData>(entity =>
            {
                entity.ToTable("telemetry_data");

                entity.Property(t => t.RecId).HasColumnName("rec_id");

                entity.Property(t => t.RecDatetime).HasColumnName("rec_datetime");

                entity.Property(t => t.OBDIIPIDId).HasColumnName("OBDII_PID_id");

                entity.Property(t => t.EcuId).HasColumnName("ECU_id");

                entity.Property(t => t.ResponseDlc).HasColumnName("response_dlc");

                entity.Property(t => t.Response).HasColumnName("response");

                entity.Property(t => t.TripId).HasColumnName("trip_id");

                entity.HasKey(t => t.RecId);

                entity.HasOne(t => t.Trip).WithMany().HasForeignKey(t => t.TripId).OnDelete(DeleteBehavior.Cascade);

                entity.HasOne(t => t.OBDIIPID).WithMany().HasForeignKey(t => t.OBDIIPIDId).OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<GPSData>(entity =>
            {
                entity.ToTable("GPS_data");

                entity.Property(g => g.RecId).HasColumnName("rec_id");

                entity.Property(g => g.RecDatetime).HasColumnName("rec_datetime");

                entity.Property(g => g.TripId).HasColumnName("trip_id");

                entity.Property(g => g.LatitudeDeg).HasColumnName("latitude_deg");

                entity.Property(g => g.LongitudeDeg).HasColumnName("longitude_deg");

                entity.Property(g => g.AccuracyM).HasColumnName("accuracy_m");

                entity.Property(g => g.SpeedKmh).HasColumnName("speed_kmh");

                entity.Property(g => g.BearingDeg).HasColumnName("bearing_deg");

                entity.HasKey(g => g.RecId);

                entity.HasOne(g => g.Trip).WithMany().HasForeignKey(t => t.TripId).OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<RefreshToken>(entity =>
            {
                entity.ToTable("refresh_tokens");

                entity.Property(rt => rt.TokenId).HasColumnName("token_id");

                entity.Property(rt => rt.TokenHash).HasColumnName("token_hash");

                entity.Property(rt => rt.Expires).HasColumnName("expires");

                entity.Property(rt => rt.IsRevoked).HasColumnName("is_revoked");

                entity.Property(rt => rt.PersonId).HasColumnName("person_id");

                entity.HasKey(g => g.TokenId);

                entity.HasOne(g => g.Person).WithMany().HasForeignKey(t => t.PersonId).OnDelete(DeleteBehavior.Cascade);

                entity.HasIndex(g => g.TokenHash).IsUnique();
            });

            modelBuilder.Entity<Avatar>(entity =>
            {
                entity.ToTable("avatars");

                entity.Property(a => a.AvatarId).HasColumnName("avatar_id");

                entity.Property(a => a.CreatedAt).HasColumnName("created_at");

                entity.Property(a => a.AvatarUrl).HasColumnName("avatar_url");

                entity.Property(a => a.PersonId).HasColumnName("person_id");

                entity.Property(a => a.ContentType).HasColumnName("content_type");

                entity.HasKey(a => a.AvatarId);

                entity.HasOne(a => a.Person).WithMany().HasForeignKey(p => p.PersonId).OnDelete(DeleteBehavior.Cascade);
            });

            modelBuilder.Entity<CarPhoto>(entity =>
            {
                entity.ToTable("car_photos");

                entity.Property(a => a.PhotoId).HasColumnName("photo_id");

                entity.Property(a => a.CreatedAt).HasColumnName("created_at");

                entity.Property(a => a.PhotoUrl).HasColumnName("photo_url");

                entity.Property(a => a.CarId).HasColumnName("car_id");

                entity.Property(a => a.ContentType).HasColumnName("content_type");

                entity.HasKey(a => a.PhotoId);

                entity.HasOne(a => a.Car).WithMany().HasForeignKey(c => c.CarId).OnDelete(DeleteBehavior.Cascade);
            });
        }
    }
}
