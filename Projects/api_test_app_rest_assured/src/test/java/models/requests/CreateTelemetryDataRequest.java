package models.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class CreateTelemetryDataRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recDatetime;
    private Integer serviceId;
    private Integer PID;
    private Byte[] ECUId;
    private Integer responseDlc;
    private Byte[] response;
    private Long tripId;
}
