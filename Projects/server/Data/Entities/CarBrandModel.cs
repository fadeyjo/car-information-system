namespace server.Models.Entities
{
    public class CarBrandModel
    {
        public uint CarBrandModelId { get; set; }
        public string ModelName { get; set; } = null!;
        public ushort BrandId { get; set; }
        public CarBrand CarBrand { get; set; } = null!;
    }
}
