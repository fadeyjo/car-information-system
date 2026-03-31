package tests;

import api.PersonApi;
import factories.PersonFactory;
import io.restassured.http.ContentType;
import models.responses.PersonDto;
import models.responses.TokensDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PersonTest  {

    private static Integer startOperatorId;
    private static TokensDto startOperatorTokens;

    private final static List<Integer> createdIds = new ArrayList<>();

    private static Stream<Arguments> anyRolesStream() {
        return Stream.of(
                Arguments.of("OPERATOR", 204),
                Arguments.of("USER", 403)
        );
    }

    private static Stream<Arguments> getInvalidRegisterScenarios() {
        return Stream.of(
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.EMAIL, 400),
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.PHONE, 400),
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.DRIVE_LICENSE, 400),
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.EMPTY_FIRST_NAME, 400),
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.PASSWORD_TOO_SHORT, 400),
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.NON_EXISTENT_ROLE_ID, 404),
                Arguments.of(PersonFactory.InvalidRegisterUserScenario.BIRTH_IN_FUTURE, 400)
        );
    }

    private static Stream<PersonFactory.InvalidUpdateUserScenario> getInvalidUpdateScenarios() {
        return Arrays.stream(PersonFactory.InvalidUpdateUserScenario.values());
    }

    @Test
    void testRegisterPerson() {

        var request = PersonFactory.getUser();

        var response = PersonApi.registerPerson(request)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdIds.add(response.getPersonId());

        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getPhone()).isEqualTo(request.getPhone());
        assertThat(response.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(response.getLastName()).isEqualTo(request.getLastName());
        assertThat(response.getDriveLicense()).isEqualTo(request.getDriveLicense());
    }

    @Test
    void testLoginPerson() {

        var user = PersonFactory.getUser();

        var userData = PersonApi.registerPerson(user)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdIds.add(userData.getPersonId());

        PersonApi.loginPerson(user.getEmail(), user.getPassword())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void testGetUserData() {

        var user = PersonFactory.getUser();

        PersonDto created = PersonApi.registerPerson(user)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdIds.add(created.getPersonId());

        var login = PersonApi.loginPerson(user.getEmail(), user.getPassword())
                .then()
                .statusCode(200)
                .extract()
                .as(TokensDto.class);

        var actual = PersonApi.getPersonData(login.getAccessToken())
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto.class);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(created);
    }

    @ParameterizedTest
    @MethodSource("anyRolesStream")
    public void testDeleteUsers(String role, Integer expectedStatusCode) {
        var regData = switch (role) {
            case "OPERATOR" -> PersonFactory.getOperator();
            case "USER" -> PersonFactory.getUser();
            default -> throw new IllegalArgumentException("Unknown role");
        };

        var person = PersonApi.registerPerson(regData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        if (expectedStatusCode != 204) {
            createdIds.add(person.getPersonId());
        }

        var tokens = PersonApi.loginPerson(person.getEmail(), regData.getPassword())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TokensDto.class);

        PersonApi.deletePerson(tokens.getAccessToken(), person.getPersonId())
                .then()
                .statusCode(expectedStatusCode);
    }

    @ParameterizedTest
    @MethodSource("getInvalidRegisterScenarios")
    public void testRegistrationWithInvalidScenarios(PersonFactory.InvalidRegisterUserScenario scenario, Integer expectedStatusCode) {
        var regData = PersonFactory.getInvalidUser(scenario);

        PersonApi.registerPerson(regData)
                .then()
                .statusCode(expectedStatusCode)
                .contentType(ContentType.JSON);
    }

    @Test
    public void testUpdateUser() {
        var userRegData = PersonFactory.getUser();

        var user = PersonApi.registerPerson(userRegData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdIds.add(user.getPersonId());

        var tokens = PersonApi.loginPerson(userRegData.getEmail(), userRegData.getPassword())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TokensDto.class);

        var updateData = PersonFactory.getUpdatedData();

        PersonApi.updatePerson(tokens.getAccessToken(), updateData)
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @MethodSource("getInvalidUpdateScenarios")
    public void testUpdateWithInvalidScenarios(PersonFactory.InvalidUpdateUserScenario s) {
        var personRegData = PersonFactory.getUser();

        var person = PersonApi.registerPerson(personRegData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdIds.add(person.getPersonId());

        var tokens = PersonApi.loginPerson(personRegData.getEmail(), personRegData.getPassword())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TokensDto.class);

        var updatedData = PersonFactory.getInvalidUpdatedData(s);

        PersonApi.updatePerson(tokens.getAccessToken(), updatedData)
                .then()
                .statusCode(400);
    }

    @BeforeAll
    public static void setup() {
        var operator = PersonFactory.getOperator();

        var person = PersonApi.registerPerson(operator)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        startOperatorId = person.getPersonId();

        startOperatorTokens = PersonApi.loginPerson(operator.getEmail(), operator.getPassword())
                .then()
                .extract()
                .as(TokensDto.class);
    }

    @AfterAll
    public static void cleanup() {
        createdIds.add(startOperatorId);

        createdIds.forEach(id ->
                PersonApi.deletePerson(startOperatorTokens.getAccessToken(), id)
                        .then()
                        .statusCode(204)
        );
    }
}
