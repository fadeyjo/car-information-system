package api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import models.requests.EndTripRequest;
import models.requests.StartTripRequest;

import static io.restassured.RestAssured.given;

public class TripApi extends BaseApi {
    public static Response startTrip(String accessToken, StartTripRequest body) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/trips")
                .header(auth(accessToken))
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/start");
    }

    public static Response getTrpById(String accessToken, Long tripId) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/trips")
                .header(auth(accessToken))
                .pathParam("tripId", tripId)
                .when()
                .get("/{tripId}");
    }

    public static Response endTrip(String accessToken, EndTripRequest body) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/trips")
                .header(auth(accessToken))
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/end");
    }

    public static Response deleteTripById(String accessToken, Long tripId) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/trips")
                .header(auth(accessToken))
                .pathParam("tripId", tripId)
                .when()
                .delete("/{tripId}");
    }
}
