package api;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import models.requests.SignInRequest;
import models.requests.SignUpRequest;
import models.requests.UpdatePersonInfoRequest;

import static io.restassured.RestAssured.given;

public class PersonApi extends BaseApi {

    private static Header auth(String token) {
        return new Header("Authorization", "Bearer " + token);
    }

    public static Response registerPerson(SignUpRequest person) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/persons")
                .contentType(ContentType.JSON)
                .body(person)
                .when()
                .post();
    }

    public static Response loginPerson(String email, String password) {
        var body = SignInRequest
                .builder()
                .email(email)
                .password(password)
                .build();

        return given()
                .baseUri(BASE_URI)
                .basePath("/refresh-tokens")
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/login");
    }

    public static Response getPersonData(String accessToken) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/persons")
                .header(auth(accessToken))
                .when()
                .get();
    }

    public static Response updatePerson(String accessToken, UpdatePersonInfoRequest updatedData) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/persons")
                .header(auth(accessToken))
                .contentType(ContentType.JSON)
                .body(updatedData)
                .when()
                .put();
    }

    public static Response deletePerson(String accessToken, Integer personId) {
        return given()
                .baseUri(BASE_URI)
                .basePath("/persons")
                .header(auth(accessToken))
                .pathParam("personId", personId)
                .when()
                .delete("/{personId}");
    }
}
