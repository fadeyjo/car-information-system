namespace server.Models.Entities
{
    public class CarPhoto
    {
        public uint PhotoId { get; set; }
        public DateTime CreatedAt { get; set; }
        public string PhotoUrl { get; set; } = null!;
        public uint CarId { get; set; }
        public string ContentType { get; set; } = null!;
        public Car Car { get; set; } = null!;
    }
}
