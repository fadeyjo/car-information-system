package tests.integration;

import api.TripApi;
import factories.PersonFactory;
import factories.TripFactory;
import io.restassured.http.ContentType;
import models.responses.TokensDto;
import models.responses.TripDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import steps.CarSteps;
import steps.PersonSteps;
import steps.TripSteps;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TripTest {
    private static TokensDto startOperatorTokens;

    private final static List<Integer> createdPersonIds = new ArrayList<>();
    private final static List<Integer> createdCarsIds = new ArrayList<>();
    private final static List<Long> createdTripsIds = new ArrayList<>();

    @Test
    public void testStartTrip() {
        var user = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(user);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var car = CarSteps.createCar(tokens.getAccessToken());

        createdCarsIds.add(car.getCarId());

        var startTripData = TripFactory.getStartTripData(car.getCarId());

        var trip = TripApi.startTrip(tokens.getAccessToken(), startTripData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(TripDto.class);

        createdTripsIds.add(trip.getTripId());

        assertThat(trip.getCarId()).isEqualTo(car.getCarId());
        assertThat(trip.getStartDatetime()).isEqualTo(startTripData.getStartDateTime());
    }

    @Test
    public void testEndTrip() throws InterruptedException {
        var user = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(user);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var car = CarSteps.createCar(tokens.getAccessToken());

        createdCarsIds.add(car.getCarId());

        var startTripData = TripFactory.getStartTripData(car.getCarId());

        var trip = TripSteps.startTrip(tokens.getAccessToken(), startTripData);

        createdTripsIds.add(trip.getTripId());

        Thread.sleep(1000);

        var endTripData = TripFactory.getEndTripData(trip.getTripId());

        TripApi.endTrip(tokens.getAccessToken(), endTripData)
                .then()
                .statusCode(204);
    }

    @Test
    public void testGetTrip() {
        var user = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(user);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var car = CarSteps.createCar(tokens.getAccessToken());

        createdCarsIds.add(car.getCarId());

        var startTripData = TripFactory.getStartTripData(car.getCarId());

        var createdTrip = TripSteps.startTrip(tokens.getAccessToken(), startTripData);

        createdTripsIds.add(createdTrip.getTripId());

        var trip = TripApi.getTrpById(tokens.getAccessToken(), createdTrip.getTripId())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TripDto.class);

        assertThat(createdTrip).usingRecursiveComparison().isEqualTo(trip);
    }

    @Test
    public void testDeleteTrip() {
        var user = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var tokens = PersonSteps.registerAndLogin(user);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var car = CarSteps.createCar(tokens.getAccessToken());

        createdCarsIds.add(car.getCarId());

        var startTripData = TripFactory.getStartTripData(car.getCarId());

        var createdTrip = TripSteps.startTrip(tokens.getAccessToken(), startTripData);

        TripApi.deleteTripById(tokens.getAccessToken(), createdTrip.getTripId())
                .then()
                .statusCode(204);
    }

    @BeforeAll
    public static void setup() {
        var operator = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        startOperatorTokens = PersonSteps.registerAndLogin(operator);
    }

    @AfterAll
    public static void cleanup() {
        createdPersonIds.add(startOperatorTokens.getPerson().getPersonId());

        createdTripsIds.forEach(id ->
                TripSteps.deleteTripById(startOperatorTokens.getAccessToken(), id)
        );

        createdCarsIds.forEach(id ->
                CarSteps.deleteCarById(startOperatorTokens.getAccessToken(), id)
        );

        createdPersonIds.forEach(id ->
                PersonSteps.deletePerson(startOperatorTokens.getAccessToken(), id)
        );
    }
}
