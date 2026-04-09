package models.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CreateGpsDataRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recDatetime;
    private Long tripId;
    private Double latitudeDeg;
    private Double longitudeDeg;
    private Double accuracyM;
    private Integer speedKmh;
    private Integer bearingDeg;
}
