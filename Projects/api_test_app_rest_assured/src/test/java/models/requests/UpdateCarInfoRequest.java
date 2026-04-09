package models.requests;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateCarInfoRequest {
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
}
