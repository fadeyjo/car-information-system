namespace server.Contracts.Responses
{
    public class AvatarDto
    {
        public uint AvatarId { get; set; }
        public string AvatarUrl { get; set; } = null!;
        public uint PersonId { get; set; }
        public string ContentType { get; set; } = null!;
    }
}
