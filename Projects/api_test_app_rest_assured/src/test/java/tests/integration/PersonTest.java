package tests.integration;

import api.PersonApi;
import factories.PersonFactory;
import io.restassured.http.ContentType;
import models.responses.PersonDto;
import models.responses.TokensDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import steps.PersonSteps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PersonTest  {

    private static TokensDto startOperatorTokens;

    private final static List<Integer> createdPersonIds = new ArrayList<>();

    private static Stream<Arguments> anyRolesStream() {
        return Stream.of(
                Arguments.of(PersonFactory.Role.USER, 403),
                Arguments.of(PersonFactory.Role.OPERATOR, 204)
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

        var request = PersonFactory.getPerson();

        var response = PersonApi.registerPerson(request)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdPersonIds.add(response.getPersonId());

        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getPhone()).isEqualTo(request.getPhone());
        assertThat(response.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(response.getLastName()).isEqualTo(request.getLastName());
        assertThat(response.getDriveLicense()).isEqualTo(request.getDriveLicense());
    }

    @Test
    void testLoginPerson() {

        var user = PersonFactory.getPerson();

        var userData = PersonApi.registerPerson(user)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);

        createdPersonIds.add(userData.getPersonId());

        PersonApi.loginPerson(user.getEmail(), user.getPassword())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void testGetPersonData() {

        var user = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(user);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var actual = PersonSteps.getPersonData(tokens.getAccessToken());

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(tokens.getPerson());
    }

    @ParameterizedTest
    @MethodSource("anyRolesStream")
    public void testDeleteUsers(PersonFactory.Role role, Integer expectedStatusCode) {
        var regData = PersonFactory.getPerson(role);

        var tokens = PersonSteps.registerAndLogin(regData);

        if (expectedStatusCode != 204) {
            createdPersonIds.add(tokens.getPerson().getPersonId());
        }

        PersonApi.deletePerson(tokens.getAccessToken(), tokens.getPerson().getPersonId())
                .then()
                .statusCode(expectedStatusCode);
    }

    @ParameterizedTest
    @MethodSource("getInvalidRegisterScenarios")
    public void testRegistrationWithInvalidScenarios(PersonFactory.InvalidRegisterUserScenario scenario, Integer expectedStatusCode) {
        var regData = PersonFactory.getInvalidUser(scenario);

        PersonApi.registerPerson(regData)
                .then()
                .statusCode(expectedStatusCode);
    }

    @Test
    public void testUpdateUser() {
        var userRegData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(userRegData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var updateData = PersonFactory.getUpdatedData();

        PersonApi.updatePerson(tokens.getAccessToken(), updateData)
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @MethodSource("getInvalidUpdateScenarios")
    public void testUpdateWithInvalidScenarios(PersonFactory.InvalidUpdateUserScenario s) {
        var personRegData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personRegData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var updatedData = PersonFactory.getInvalidUpdatedData(s);

        PersonApi.updatePerson(tokens.getAccessToken(), updatedData)
                .then()
                .statusCode(400);
    }

    @BeforeAll
    public static void setup() {
        var operator = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        startOperatorTokens = PersonSteps.registerAndLogin(operator);
    }

    @AfterAll
    public static void cleanup() {
        createdPersonIds.add(startOperatorTokens.getPerson().getPersonId());

        createdPersonIds.forEach(id ->
                PersonSteps.deletePerson(
                        startOperatorTokens.getAccessToken(),
                        id
                )
        );
    }
}
