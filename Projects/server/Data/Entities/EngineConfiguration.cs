namespace server.Models.Entities
{
    public class EngineConfiguration
    {
        public uint EngineConfigId { get; set; }
        public ushort EnginePowerHp { get; set; }
        public float EnginePowerKw { get; set; }
        public float EngineCapacityL { get; set; }
        public byte TankCapacityL { get; set; }
        public byte FuelTypeId { get; set; }
        public FuelType FuelType { get; set; } = null!;
    }
}
