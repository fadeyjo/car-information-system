package api;

import io.restassured.http.Header;

public class BaseApi {
    protected static final String BASE_URI = "http://localhost:5055/api";

    protected static Header auth(String token) {
        return new Header("Authorization", "Bearer " + token);
    }
}
