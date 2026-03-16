namespace server.Contracts.Responses
{
    public class PersonDto
    {
        public uint PersonId { get; set; }
        public string Email { get; set; } = null!;
        public string Phone { get; set; } = null!;
        public string LastName { get; set; } = null!;
        public string FirstName { get; set; } = null!;
        public string? Patronymic { get; set; }
        public DateOnly Birth {  get; set; }
        public string? DriveLicense { get; set; }
        public byte RoleId { get; set; }
    }
}
