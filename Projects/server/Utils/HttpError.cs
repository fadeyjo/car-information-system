namespace server.Utils
{
    public class HttpError : Exception
    {
        public string Title { get; set; } = null!;
        public int StatusCode { get; set; }

        public HttpError(string title, int statusCode)
        {
            Title = title;
            StatusCode = statusCode;
        }
    }
}
