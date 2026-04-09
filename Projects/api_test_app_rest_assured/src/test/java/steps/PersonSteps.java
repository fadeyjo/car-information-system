package steps;

import api.PersonApi;
import factories.PersonFactory;
import io.restassured.http.ContentType;
import models.requests.SignUpRequest;
import models.responses.PersonDto;
import models.responses.TokensDto;

public class PersonSteps {
    public static TokensDto registerAndLogin(SignUpRequest personData) {
        register(personData);

        return login(personData.getEmail(), personData.getPassword());
    }

    public static TokensDto registerAndLogin(PersonFactory.Role role) {
        SignUpRequest personData = PersonFactory.getPerson(role);

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

    public static PersonDto register(PersonFactory.Role role) {
        SignUpRequest personData = PersonFactory.getPerson(role);

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
}
