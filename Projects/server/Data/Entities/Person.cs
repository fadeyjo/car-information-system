namespace server.Models.Entities
{
    public class Person
    {
        public uint PersonId { get; set; }
        public DateTime CreatedAt { get; set; }
        public string Email { get; set; } = null!;
        public string Phone { get; set; } = null!;
        public string LastName { get; set; } = null!;
        public string FirstName { get; set; } = null!;
        public string? Patronymic { get; set; }
        public DateOnly Birth { get; set; }
        public string HashedPassword { get; set; } = null!;
        public string? DriveLicense { get; set; }
        public byte RoleId { get; set; }
        public Role Role { get; set; } = null!;
    }
}
