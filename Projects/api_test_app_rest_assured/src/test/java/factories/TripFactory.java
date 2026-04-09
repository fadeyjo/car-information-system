package factories;

import models.requests.EndTripRequest;
import models.requests.StartTripRequest;
import utils.RandomTripDataUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class TripFactory {
    public static StartTripRequest getStartTripData(Integer carId) {
        return StartTripRequest.builder()
                .startDateTime(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS))
                .carId(carId)
                .macAddress(RandomTripDataUtil.getMacAddress())
                .build();
    }

    public static StartTripRequest getStartTripData(Integer carId, LocalDateTime startDatetime) {
        var data = getStartTripData(carId);

        data.setStartDateTime(startDatetime.truncatedTo(ChronoUnit.SECONDS));

        return data;
    }

    public static StartTripRequest getStartTripData(Integer carId, LocalDateTime startDatetime, String macAddress) {
        var data = getStartTripData(carId);

        data.setStartDateTime(startDatetime.truncatedTo(ChronoUnit.SECONDS));
        data.setMacAddress(macAddress);

        return data;
    }

    public static StartTripRequest getStartTripData(Integer carId, String macAddress) {
        var data = getStartTripData(carId);

        data.setMacAddress(macAddress);

        return data;
    }

    public static EndTripRequest getEndTripData(Long tripId) {
        return EndTripRequest.builder()
                .endDatetime(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS))
                .tripId(tripId)
                .build();
    }

    public static EndTripRequest getEndTripData(Long tripId, LocalDateTime endDatetime) {
        var data = getEndTripData(tripId);

        data.setEndDatetime(endDatetime.truncatedTo(ChronoUnit.SECONDS));

        return data;
    }
}
