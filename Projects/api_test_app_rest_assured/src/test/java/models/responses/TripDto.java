package models.responses;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import utils.LocalDateTimeJsonDeserializer;

import java.time.LocalDateTime;

@Getter
@Setter
public class TripDto {
    private Long TripId;
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    private LocalDateTime StartDatetime;
    private Integer DeviceId;
    private Integer CarId;
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    private LocalDateTime EndDatetime;
}
