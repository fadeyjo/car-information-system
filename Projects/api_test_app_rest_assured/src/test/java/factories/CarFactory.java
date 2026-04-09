package factories;

import models.requests.CreateCarRequest;
import models.requests.UpdateCarInfoRequest;
import net.datafaker.Faker;
import utils.RandomCarDataUtil;

import java.time.LocalDate;

public class CarFactory {

    public enum InvalidCreateCarScenario {
        VIN_NUMBER,
        STATE_NUMBER,
        NON_EXISTENT_BRAND_NAME,
        NON_EXISTENT_MODEL_NAME,
        RELEASE_YEAR,
        NON_EXISTENT_GEARBOX_NAME,
        NON_EXISTENT_DRIVE_NAME,
        VEHICLE_WEIGHT_KG,
        ENGINE_POWER_HP,
        ENGINE_POWER_KW,
        ENGINE_CAPACITY_L,
        TANK_CAPACITY_L,
        NON_EXISTENT_FUEL_TYPE_NAME
    }

    public enum InvalidUpdateCarScenario {
        VIN_NUMBER,
        STATE_NUMBER,
        NON_EXISTENT_BRAND_NAME,
        NON_EXISTENT_MODEL_NAME,
        RELEASE_YEAR,
        NON_EXISTENT_GEARBOX_NAME,
        NON_EXISTENT_DRIVE_NAME,
        VEHICLE_WEIGHT_KG,
        ENGINE_POWER_HP,
        ENGINE_POWER_KW,
        ENGINE_CAPACITY_L,
        TANK_CAPACITY_L,
        NON_EXISTENT_FUEL_TYPE_NAME
    }

    public static CreateCarRequest getInvalidCar(InvalidCreateCarScenario s) {
        var car = getCar();

        switch (s) {
            case VIN_NUMBER -> car.setVinNumber("invalid");

            case STATE_NUMBER -> car.setStateNumber("и131фф3333");

            case NON_EXISTENT_BRAND_NAME -> car.setBrandName("some inv brand 1234");

            case NON_EXISTENT_MODEL_NAME -> car.setModelName("some inv model 1234");

            case RELEASE_YEAR -> car.setReleaseYear(1992);

            case NON_EXISTENT_GEARBOX_NAME -> car.setGearboxName("some inv gear 1234");

            case NON_EXISTENT_DRIVE_NAME -> car.setDriveName("some inv dr 1234");

            case VEHICLE_WEIGHT_KG -> car.setVehicleWeightKg(300);

            case ENGINE_POWER_HP -> car.setEnginePowerHp(3);

            case ENGINE_POWER_KW -> car.setEnginePowerKw(3001.0);

            case ENGINE_CAPACITY_L -> car.setEngineCapacityL(76.0);

            case TANK_CAPACITY_L -> car.setTankCapacityL(255);

            case NON_EXISTENT_FUEL_TYPE_NAME -> car.setFuelTypeName("some inv fuel 1234");

            default -> throw new IllegalArgumentException("Unknown scenario");
        }

        return car;
    }

    public static UpdateCarInfoRequest getInvalidUpdateCarData(InvalidUpdateCarScenario s) {
        var car = getUpdateCarData();

        switch (s) {
            case VIN_NUMBER -> car.setVinNumber("invalid");

            case STATE_NUMBER -> car.setStateNumber("и131фф3333");

            case NON_EXISTENT_BRAND_NAME -> car.setBrandName("some inv brand 1234");

            case NON_EXISTENT_MODEL_NAME -> car.setModelName("some inv model 1234");

            case RELEASE_YEAR -> car.setReleaseYear(1992);

            case NON_EXISTENT_GEARBOX_NAME -> car.setGearboxName("some inv gear 1234");

            case NON_EXISTENT_DRIVE_NAME -> car.setDriveName("some inv dr 1234");

            case VEHICLE_WEIGHT_KG -> car.setVehicleWeightKg(300);

            case ENGINE_POWER_HP -> car.setEnginePowerHp(3);

            case ENGINE_POWER_KW -> car.setEnginePowerKw(3001.0);

            case ENGINE_CAPACITY_L -> car.setEngineCapacityL(76.0);

            case TANK_CAPACITY_L -> car.setTankCapacityL(255);

            case NON_EXISTENT_FUEL_TYPE_NAME -> car.setFuelTypeName("some inv fuel 1234");

            default -> throw new IllegalArgumentException("Unknown scenario");
        }

        return car;
    }

    public static CreateCarRequest getCar() {
        var faker = new Faker();

        String vinNumber = RandomCarDataUtil.getRandomVinNumber(faker);
        String stateNumber = RandomCarDataUtil.getRandomStateNumber(faker);
        String brandName = RandomCarDataUtil.getRandomBrandName();
        String modelName = RandomCarDataUtil.getRandomModelName(brandName);
        String bodyName = RandomCarDataUtil.getRandomBodyName();
        Integer releaseYear = RandomCarDataUtil.getRandomReleaseYear(faker);
        String gearboxName = RandomCarDataUtil.getRandomGearboxName();
        String driveName = RandomCarDataUtil.getRandomDriveName();
        Integer vehicleWeightKg = RandomCarDataUtil.getRandomVehicleWeightKg(faker);
        Integer enginePowerHp = RandomCarDataUtil.getRandomEnginePowerHp(faker);
        Double enginePowerKw = RandomCarDataUtil.getRandomEnginePowerKw(faker);
        Double engineCapacityL = RandomCarDataUtil.getRandomEngineCapacityL(faker);
        Integer tankCapacityL = RandomCarDataUtil.getRandomTankCapacityL(faker);
        String fuelTypeName = RandomCarDataUtil.getRandomFuelTypeName();

        return CreateCarRequest.builder()
                .vinNumber(vinNumber)
                .stateNumber(stateNumber)
                .brandName(brandName)
                .modelName(modelName)
                .bodyName(bodyName)
                .releaseYear(releaseYear)
                .gearboxName(gearboxName)
                .driveName(driveName)
                .vehicleWeightKg(vehicleWeightKg)
                .enginePowerHp(enginePowerHp)
                .enginePowerKw(enginePowerKw)
                .engineCapacityL(engineCapacityL)
                .tankCapacityL(tankCapacityL)
                .fuelTypeName(fuelTypeName)
                .build();
    }

    public static UpdateCarInfoRequest getUpdateCarData() {
        var faker = new Faker();

        String vinNumber = RandomCarDataUtil.getRandomVinNumber(faker);
        String stateNumber = RandomCarDataUtil.getRandomStateNumber(faker);
        String brandName = RandomCarDataUtil.getRandomBrandName();
        String modelName = RandomCarDataUtil.getRandomModelName(brandName);
        String bodyName = RandomCarDataUtil.getRandomBodyName();
        Integer releaseYear = RandomCarDataUtil.getRandomReleaseYear(faker);
        String gearboxName = RandomCarDataUtil.getRandomGearboxName();
        String driveName = RandomCarDataUtil.getRandomDriveName();
        Integer vehicleWeightKg = RandomCarDataUtil.getRandomVehicleWeightKg(faker);
        Integer enginePowerHp = RandomCarDataUtil.getRandomEnginePowerHp(faker);
        Double enginePowerKw = RandomCarDataUtil.getRandomEnginePowerKw(faker);
        Double engineCapacityL = RandomCarDataUtil.getRandomEngineCapacityL(faker);
        Integer tankCapacityL = RandomCarDataUtil.getRandomTankCapacityL(faker);
        String fuelTypeName = RandomCarDataUtil.getRandomFuelTypeName();

        return UpdateCarInfoRequest.builder()
                .vinNumber(vinNumber)
                .stateNumber(stateNumber)
                .brandName(brandName)
                .modelName(modelName)
                .bodyName(bodyName)
                .releaseYear(releaseYear)
                .gearboxName(gearboxName)
                .driveName(driveName)
                .vehicleWeightKg(vehicleWeightKg)
                .enginePowerHp(enginePowerHp)
                .enginePowerKw(enginePowerKw)
                .engineCapacityL(engineCapacityL)
                .tankCapacityL(tankCapacityL)
                .fuelTypeName(fuelTypeName)
                .build();
    }
}
