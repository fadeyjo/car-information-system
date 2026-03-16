namespace server.Contracts.Requests
{
    public class CreateCarRequest
    {
        public string VinNumber { get; set; } = null!;
        public string? StateNumber { get; set; }
        public string BrandName { get; set; } = null!;
        public string ModelName { get; set; } = null!;
        public string BodyName { get; set; } = null!;
        public ushort ReleaseYear { get; set; }
        public string GearboxName { get; set; } = null!;
        public string DriveName { get; set; } = null!;
        public ushort VehicleWeightKg { get; set; }
        public ushort EnginePowerHp { get; set; }
        public float EnginePowerKw { get; set; }
        public float EngineCapacityL { get; set; }
        public byte TankCapacityL { get; set; }
        public string FuelTypeName { get; set; } = null!;
    }
}
