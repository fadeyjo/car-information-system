namespace server.Contracts.Responses
{
    public class PidsDetailDto
    {
        public List<ushort> Once { get; set; } = null!;

        public List<ushort> Repeatable { get; set; } = null!;
    }
}
