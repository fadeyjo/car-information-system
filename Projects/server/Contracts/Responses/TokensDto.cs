namespace server.Contracts.Responses
{
    public class TokensDto
    {
        public PersonDto Person { get; set; } = null!;
        public string AccessToken { get; set; } = null!;
        public string RefreshToken { get; set; } = null!;
    }
}
