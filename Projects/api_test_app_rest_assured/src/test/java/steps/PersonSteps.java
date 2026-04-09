package steps;

import api.PersonApi;
import factories.PersonFactory;
import io.restassured.http.ContentType;
import models.requests.SignUpRequest;
import models.requests.UpdatePersonInfoRequest;
import models.responses.PersonDto;
import models.responses.TokensDto;

public class PersonSteps {
    public static TokensDto registerAndLogin(SignUpRequest personData) {
        register(personData);

        return login(personData.getEmail(), personData.getPassword());
    }

    public static PersonDto register(SignUpRequest personData) {
        return PersonApi.registerPerson(personData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(PersonDto.class);
    }

    public static TokensDto login(String email, String password) {
        return PersonApi.loginPerson(email, password)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TokensDto.class);
    }

    public static PersonDto getPersonData(String accessToken) {
        return PersonApi.getPersonData(accessToken)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto.class);
    }

    public static void deletePerson(String accessToken, Integer personId) {
        PersonApi.deletePerson(accessToken, personId)
                .then()
                .statusCode(204);
    }

    public static void updatePerson(String accessToken, UpdatePersonInfoRequest updatedData) {
        PersonApi.updatePerson(accessToken, updatedData)
                .then()
                .statusCode(204);
    }

    public static void updatePerson(String accessToken) {
        var updatedData = PersonFactory.getUpdatedData();

        updatePerson(accessToken, updatedData);
    }
}
