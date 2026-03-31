package utils;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import lombok.Getter;
import lombok.Setter;
import models.requests.SignInRequest;
import models.requests.SignUpRequest;
import models.responses.PersonDto;
import models.responses.TokensDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

@Setter
@Getter
public class AuthService extends BaseApi {
    private static SignUpRequest userRegisterRequest;
    private static SignUpRequest operatorRegisterRequest;

    private static PersonDto user;
    private static PersonDto operator;

    private static TokensDto userTokens;
    private static TokensDto operatorTokens;

    protected static Header userAuthHeader;
    protected static Header operatorAuthHeader;

    @BeforeAll
    public static void registerAndLoginsPersons() {
        System.out.println("start");

        registerPerson(1);
        registerPerson(2);

        loginPerson(userRegisterRequest);
        loginPerson(operatorRegisterRequest);
    }

    @AfterAll
    public static void deletePersons() {
        System.out.println("end");

        deletePerson(user.getPersonId());
        deletePerson(operator.getPersonId());

        userRegisterRequest = null;
        operatorRegisterRequest = null;

        user = null;
        operator = null;

        userTokens = null;
        operatorTokens = null;

        userAuthHeader = null;
        operatorAuthHeader = null;
    }

    private static void deletePerson(Integer personId) {
        given()
                .baseUri(BASE_URI)
                .basePath("/persons")
                .header(operatorAuthHeader)
                .pathParam("personId", personId)
                .when()
                .delete("/{personId}")
                .then()
                .statusCode(204);
    }

    private static void registerPerson(Integer roleId) {
        String email = getRandomStringWithLength() + "@gmail.com";
        String phone = getRandomPhone();
        String lastName = getRandomCapString();
        String firstName = getRandomCapString();
        String patronymic = getRandomCapStringOrNull();
        String password = getRandomStringWithLength(16);
        String driveLicense = getRandomDriveLicense();
        LocalDate birth = getRandomBirthByMinAge();

        var registerRequest = SignUpRequest
                .builder()
                .email(email)
                .phone(phone)
                .lastName(lastName)
                .firstName(firstName)
                .patronymic(patronymic)
                .password(password)
                .driveLicense(driveLicense)
                .birth(birth)
                .roleId(roleId)
                .build();

        var registerResponse = given()
                .baseUri(BASE_URI)
                .basePath("/persons")
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .when()
                .post()
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        if (roleId == 1) {
            userRegisterRequest = registerRequest;

            user = registerResponse;

            return;
        }

        operatorRegisterRequest = registerRequest;

        operator = registerResponse;
    }

    private static void loginPerson(SignUpRequest personData) {
        String email = personData.getEmail();
        String password = personData.getPassword();

        var body = SignInRequest
                .builder()
                .email(email)
                .password(password)
                .build();

        var loginRequest = given()
                .baseUri(BASE_URI)
                .basePath("/refresh-tokens")
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TokensDto.class);

        if (personData.getRoleId() == 1) {
            userTokens = loginRequest;

            userAuthHeader = new Header("Authorization", "Bearer " + userTokens.getAccessToken());

            return;
        }

        operatorTokens = loginRequest;

        operatorAuthHeader = new Header("Authorization", "Bearer " + operatorTokens.getAccessToken());
    }

    public static LocalDate getRandomBirthByMinAge(Integer minAge) {
        LocalDate now = LocalDate.now();

        LocalDate maxDate = now.minusYears(minAge);

        LocalDate minDate = now.minusYears(100);

        long minDay = minDate.toEpochDay();
        long maxDay = maxDate.toEpochDay();

        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay + 1);

        return LocalDate.ofEpochDay(randomDay);
    }

    public static LocalDate getRandomBirthByMinAge() {
        return getRandomBirthByMinAge(18);
    }

    private static String getRandomStringWithLength(Integer length) {
        String random;

        do {
            random = UUID.randomUUID().toString().replace("-", "");
        } while (random.length() < length);

        return random.substring(0, length);
    }

    private static String getRandomStringWithLength() {
        return getRandomStringWithLength(8);
    }

    private static String getRandomPhone() {
        StringBuilder phone = new StringBuilder("+7");

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            phone.append(rand.nextInt(10));
        }

        return phone.toString();
    }

    private static String getRandomCapString(Integer length) {
        String random;

        do {
            random = UUID.randomUUID().toString().replace("-", "");
        } while (random.length() < length);

        random = random.substring(0, length).toLowerCase();

        return random.substring(0, 1).toUpperCase() + random.substring(1);
    }

    private static String getRandomCapString() {
        return getRandomCapString(8);
    }

    private static String getRandomCapStringOrNull(Integer length) {
        Random rand = new Random();

        boolean isNull = rand.nextInt() % 2 == 0;

        if (isNull) {
            return null;
        }

        return getRandomCapString(length);
    }

    private static String getRandomCapStringOrNull() {
        return getRandomCapStringOrNull(8);
    }

    private static String getRandomDriveLicense() {
        StringBuilder driveLicense = new StringBuilder();

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            driveLicense.append(rand.nextInt(10));
        }

        return driveLicense.toString();
    }
}
