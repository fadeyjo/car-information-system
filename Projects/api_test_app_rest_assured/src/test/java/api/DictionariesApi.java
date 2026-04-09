package api;

import factories.PersonFactory;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import models.requests.SignUpRequest;
import models.responses.*;

import java.util.List;

import static io.restassured.RestAssured.given;

public class DictionariesApi extends BaseApi {

    private static PersonDto registerPerson(SignUpRequest personData) {
        return PersonApi.registerPerson(personData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);
    }

    private static TokensDto loginPerson(SignUpRequest personData) {
        return PersonApi.loginPerson(personData.getEmail(), personData.getPassword())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TokensDto.class);
    }

    private static void deletePerson(String accessToken, Integer personId) {
        PersonApi.deletePerson(accessToken, personId)
                .then()
                .statusCode(204);
    }

    private static Header getAuthHeader(String accessToken) {
        return new Header("Authorization", "Bearer " + accessToken);
    }

    public static Response getAllCarBodies() {
        var personBody = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var personDto = registerPerson(personBody);

        var tokens = loginPerson(personBody);

        var data = given()
                .baseUri(BASE_URI)
                .basePath("/car-bodies")
                .header(getAuthHeader(tokens.getAccessToken()))
                .when()
                .get();

        deletePerson(tokens.getAccessToken(), personDto.getPersonId());

        return data;
    }

    public static Response getAllCarBrands() {
        var personBody = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var personDto = registerPerson(personBody);

        var tokens = loginPerson(personBody);

        var data = given()
                .baseUri(BASE_URI)
                .basePath("/car-brands")
                .header(getAuthHeader(tokens.getAccessToken()))
                .when()
                .get();

        deletePerson(tokens.getAccessToken(), personDto.getPersonId());

        return data;
    }

    public static Response getAllCarModels(String brandName) {
        var personBody = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var personDto = registerPerson(personBody);

        var tokens = loginPerson(personBody);

        var data = given()
                .baseUri(BASE_URI)
                .basePath("/car-models")
                .header(getAuthHeader(tokens.getAccessToken()))
                .pathParam("brandName", brandName)
                .when()
                .get("/{brandName}");

        deletePerson(tokens.getAccessToken(), personDto.getPersonId());

        return data;
    }

    public static Response getAllCarGearboxes() {
        var personBody = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var personDto = registerPerson(personBody);

        var tokens = loginPerson(personBody);

        var data = given()
                .baseUri(BASE_URI)
                .basePath("/car-gearboxes")
                .header(getAuthHeader(tokens.getAccessToken()))
                .when()
                .get();

        deletePerson(tokens.getAccessToken(), personDto.getPersonId());

        return data;
    }

    public static Response getAllFuelTypes() {
        var personBody = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var personDto = registerPerson(personBody);

        var tokens = loginPerson(personBody);

        var data = given()
                .baseUri(BASE_URI)
                .basePath("/fuel-types")
                .header(getAuthHeader(tokens.getAccessToken()))
                .when()
                .get();

        deletePerson(tokens.getAccessToken(), personDto.getPersonId());

        return data;
    }

    public static Response getAllCarDrives() {
        var personBody = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var personDto = registerPerson(personBody);

        var tokens = loginPerson(personBody);

        var data = given()
                .baseUri(BASE_URI)
                .basePath("/car-drives")
                .header(getAuthHeader(tokens.getAccessToken()))
                .when()
                .get();

        deletePerson(tokens.getAccessToken(), personDto.getPersonId());

        return data;
    }
}
