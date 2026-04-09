package api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import models.requests.CreateCarRequest;
import models.requests.UpdateCarInfoRequest;

import static io.restassured.RestAssured.given;

public class CarApi extends BaseApi {

    public static Response createCar(String accessToken, CreateCarRequest body) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/cars")
                .header(auth(accessToken))
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post();
    }

    public static Response updateCar(String accessToken, UpdateCarInfoRequest body, Integer carId) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/cars")
                .header(auth(accessToken))
                .pathParam("carId", carId)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/{carId}");
    }

    public static Response getCarById(String accessToken, Integer carId) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/cars")
                .header(auth(accessToken))
                .pathParam("carId", carId)
                .when()
                .get("/{carId}");
    }

    public static Response getMyCars(String accessToken) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/cars")
                .header(auth(accessToken))
                .when()
                .get("/my-cars");
    }

    public static Response deleteCar(String accessToken, Integer carId) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/cars")
                .pathParam("carId", carId)
                .header(auth(accessToken))
                .when()
                .delete("/{carId}");
    }
}
