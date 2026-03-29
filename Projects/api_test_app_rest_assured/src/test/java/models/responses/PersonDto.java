package models.responses;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import utils.LocalDateJsonDeserializer;

import java.time.LocalDate;

@Getter
@Setter
public class PersonDto {
    private Integer personId;
    private String email;
    private String phone;
    private String lastName;
    private String firstName;
    private String patronymic;
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    private LocalDate birth;
    private String driveLicense;
    private Integer roleId;
    private Integer avatarId;
}
