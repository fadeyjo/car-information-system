package api;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import models.requests.SignInRequest;
import models.requests.SignUpRequest;
import models.responses.PersonDto;
import models.responses.TokensDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;

public class TestPerson extends TestBase {

    private static PersonDto person;
    private static String accessToken;
    private static String refreshToken;
    private static Header authHeader;
    private final static String password = "1234567890";

    @BeforeAll
    public static void registerAndLogin() throws Exception {
        register(
                "test123@gmail.com", "+78127462235", "Surname",
                "First", null, LocalDate.of(2004, 2, 21),
                password, null, 2
        );

        login();
    }

    private static void register(
            String email, String phone, String lastName,
            String firstName, String patronymic, LocalDate birth,
            String password, String driveLicense, Integer roleId
    ) {
        var body = SignUpRequest
                .builder()
                .email(email)
                .phone(phone)
                .lastName(lastName)
                .firstName(firstName)
                .patronymic(patronymic)
                .birth(birth)
                .driveLicense(driveLicense)
                .password(password)
                .roleId(roleId)
                .build();

        person =  given()
                .baseUri(BASE_URL)
                .basePath("/persons")
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post()
                .then()
                .contentType(ContentType.JSON)
                .statusCode(201)
                .extract()
                .as(PersonDto.class);
    }

    private static void login() throws Exception {
        if (person == null) {
            throw new Exception("person не инициализирован");
        }

        var body = SignInRequest
                .builder()
                .email(person.getEmail())
                .password(password)
                .build();

        var tokens = given()
                .baseUri(BASE_URL)
                .basePath("/refresh-tokens")
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .as(TokensDto.class);

        accessToken = tokens.getAccessToken();
        refreshToken = tokens.getRefreshToken();
        authHeader = new Header("Authorization", "Bearer " + accessToken);
    }

    @Test
    public void testGetPerson() {
        var res = given()
                .baseUri(BASE_URL)
                .basePath("/persons")
                .header(authHeader)
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto.class);
    }

    @AfterAll
    public static void deleteRegisteredPerson() throws Exception {
        if (person == null) {
            throw new Exception("person не инициализирован");
        }

        given()
                .baseUri(BASE_URL)
                .basePath("/persons")
                .when()
                .delete()
                .then()
                .statusCode(204);
    }
}
