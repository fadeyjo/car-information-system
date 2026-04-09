package models.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarDto {
    private Integer carId;
    private String vinNumber;
    private String stateNumber;
    private String brandName;
    private String modelName;
    private String bodyName;
    private Integer releaseYear;
    private String gearboxName;
    private String driveName;
    private Integer vehicleWeightKg;
    private Integer enginePowerHp;
    private Double enginePowerKw;
    private Double engineCapacityL;
    private Integer tankCapacityL;
    private String fuelTypeName;
    private Integer personId;
    private Integer photoId;
}
