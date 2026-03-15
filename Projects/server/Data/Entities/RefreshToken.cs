namespace server.Models.Entities
{
    public class RefreshToken
    {
        public ulong TokenId { get; set; }
        public string TokenHash { get; set; } = null!;
        public DateTime Expires { get; set; }
        public bool IsRevoked { get; set; }
        public uint PersonId { get; set; }
        public Person Person { get; set; } = null!;
    }
}
