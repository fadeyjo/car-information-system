namespace server.Contracts.Responses
{
    public class CarPhotoDto
    {
        public uint PhotoId { get; set; }
        public string PhotoUrl { get; set; } = null!;
        public uint CarId { get; set; }
        public string ContentType { get; set; } = null!;
    }
}
