package steps;

import api.TripApi;
import factories.TripFactory;
import io.restassured.http.ContentType;
import models.requests.EndTripRequest;
import models.requests.StartTripRequest;
import models.responses.TripDto;

import java.time.LocalDateTime;

public class TripSteps {
    public static TripDto startTrip(String accessToken, StartTripRequest startTripData) {
        return TripApi.startTrip(accessToken, startTripData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(TripDto.class);
    }

    public static void endTrip(String accessToken, EndTripRequest endTripData) {
        TripApi.endTrip(accessToken, endTripData)
                .then()
                .statusCode(204);
    }

    public static void endTrip(String accessToken, Long tripId) {
        var endTripData = TripFactory.getEndTripData(tripId);

        TripApi.endTrip(accessToken, endTripData)
                .then()
                .statusCode(204);
    }

    public static void endTrip(String accessToken, Long tripId, LocalDateTime endDatetime) {
        var endTripData = TripFactory.getEndTripData(tripId, endDatetime);

        TripApi.endTrip(accessToken, endTripData)
                .then()
                .statusCode(204);
    }

    public static void deleteTripById(String accessToken, Long tripId) {
        TripApi.deleteTripById(accessToken, tripId)
                .then()
                .statusCode(204);
    }

    public static TripDto getTripById(String accessToken, Long tripId) {
        return TripApi.getTrpById(accessToken, tripId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(TripDto.class);
    }
}
