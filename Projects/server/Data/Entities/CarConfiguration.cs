namespace server.Models.Entities
{
    public class CarConfiguration
    {
        public uint CarConfigId { get; set; }
        public uint CarBrandModelId { get; set; }
        public byte BodyId { get; set; }
        public ushort ReleaseYear { get; set; }
        public byte GearboxId { get; set; }
        public byte DriveId { get; set; }
        public uint EngineConfId { get; set; }
        public ushort VehicleWeightKg { get; set; }
        public CarBrandModel CarBrandModel { get; set; } = null!;
        public CarBody CarBody { get; set; } = null!;
        public CarGearbox CarGearbox { get; set; } = null!;
        public CarDrive CarDrive { get; set; } = null!;
        public EngineConfiguration EngineConfiguration { get; set; } = null!;
    }
}
