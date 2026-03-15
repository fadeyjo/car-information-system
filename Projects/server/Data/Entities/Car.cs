namespace server.Models.Entities
{
    public class Car
    {
        public uint CarId { get; set; }
        public DateTime CreatedAt { get; set; }
        public uint PersonId { get; set; }
        public string VinNumber { get; set; } = null!;
        public string? StateNumber { get; set; }
        public uint CarConfigId { get; set; }
        public Person Person { get; set; } = null!;
        public CarConfiguration CarConfiguration { get; set; } = null!;
    }
}
