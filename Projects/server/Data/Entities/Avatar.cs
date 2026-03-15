namespace server.Models.Entities
{
    public class Avatar
    {
        public uint AvatarId { get; set; }
        public DateTime CreatedAt { get; set; }
        public string AvatarUrl { get; set; } = null!;
        public uint PersonId { get; set; }
        public string ContentType { get; set; } = null!;
        public Person Person { get; set; } = null!;
    }
}
