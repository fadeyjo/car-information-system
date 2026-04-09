package tests.integration;

import api.CarApi;
import factories.CarFactory;
import factories.PersonFactory;
import io.restassured.http.ContentType;
import models.requests.CreateCarRequest;
import models.requests.UpdateCarInfoRequest;
import models.responses.CarDto;
import models.responses.TokensDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import steps.CarSteps;
import steps.PersonSteps;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CarTest {

    private final static Integer CARS_COUNT_TO_TEST_MY_CARS = 1;

    private static TokensDto startOperatorTokens;

    private final static List<Integer> createdPersonIds = new ArrayList<>();
    private final static List<Integer> createdCarsIds = new ArrayList<>();

    private static Stream<Arguments> getInvalidCreateCarScenarios() {
        return Stream.of(
                Arguments.of(CarFactory.InvalidCreateCarScenario.VIN_NUMBER, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.STATE_NUMBER, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.NON_EXISTENT_BRAND_NAME, 404),
                Arguments.of(CarFactory.InvalidCreateCarScenario.NON_EXISTENT_MODEL_NAME, 404),
                Arguments.of(CarFactory.InvalidCreateCarScenario.RELEASE_YEAR, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.NON_EXISTENT_GEARBOX_NAME, 404),
                Arguments.of(CarFactory.InvalidCreateCarScenario.NON_EXISTENT_DRIVE_NAME, 404),
                Arguments.of(CarFactory.InvalidCreateCarScenario.VEHICLE_WEIGHT_KG, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.ENGINE_POWER_HP, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.ENGINE_POWER_KW, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.ENGINE_CAPACITY_L, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.TANK_CAPACITY_L, 400),
                Arguments.of(CarFactory.InvalidCreateCarScenario.NON_EXISTENT_FUEL_TYPE_NAME, 404)
        );
    }

    private static Stream<Arguments> getInvalidUpdateCarScenarios() {
        return Stream.of(
                Arguments.of(CarFactory.InvalidUpdateCarScenario.VIN_NUMBER, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.STATE_NUMBER, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.NON_EXISTENT_BRAND_NAME, 404),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.NON_EXISTENT_MODEL_NAME, 404),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.RELEASE_YEAR, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.NON_EXISTENT_GEARBOX_NAME, 404),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.NON_EXISTENT_DRIVE_NAME, 404),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.VEHICLE_WEIGHT_KG, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.ENGINE_POWER_HP, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.ENGINE_POWER_KW, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.ENGINE_CAPACITY_L, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.TANK_CAPACITY_L, 400),
                Arguments.of(CarFactory.InvalidUpdateCarScenario.NON_EXISTENT_FUEL_TYPE_NAME, 404)
        );
    }

    @Test
    public void testCreateCar() {
        var personData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var carData = CarFactory.getCar();

        var carDto = CarApi.createCar(tokens.getAccessToken(), carData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(CarDto.class);

        createdCarsIds.add(carDto.getCarId());

        assertCarData(carData, carDto);
    }

    @ParameterizedTest
    @MethodSource("getInvalidCreateCarScenarios")
    public void testCreateCarWithInvalidScenarios(CarFactory.InvalidCreateCarScenario s, Integer expectedStatusCode) {
        var personData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var carData = CarFactory.getInvalidCar(s);

        CarApi.createCar(tokens.getAccessToken(), carData)
                .then()
                .statusCode(expectedStatusCode);
    }

    @ParameterizedTest
    @MethodSource("getInvalidUpdateCarScenarios")
    public void testUpdateCarWithInvalidScenarios(CarFactory.InvalidUpdateCarScenario s, Integer expectedStatusCode) {
        var personData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var carDto = CarSteps.createCar(tokens.getAccessToken());

        createdCarsIds.add(carDto.getCarId());

        var newCarData = CarFactory.getInvalidUpdateCarData(s);

        CarApi.updateCar(tokens.getAccessToken(), newCarData, carDto.getCarId())
                .then()
                .statusCode(expectedStatusCode);
    }

    @Test
    public void testUpdateCar() {
        var personData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var carDto = CarSteps.createCar(tokens.getAccessToken());

        createdCarsIds.add(carDto.getCarId());

        var newCarData = CarFactory.getUpdateCarData();

        CarApi.updateCar(tokens.getAccessToken(), newCarData, carDto.getCarId())
                .then()
                .statusCode(204);

        var updatedCar = CarSteps.getCarDataById(tokens.getAccessToken(), carDto.getCarId());

        assertCarData(newCarData, updatedCar);
    }

    @Test
    public void testGetCar() {
        var personData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var carData = CarFactory.getCar();

        var carDto = CarSteps.createCar(tokens.getAccessToken(), carData);

        createdCarsIds.add(carDto.getCarId());

        var car = CarApi.getCarById(tokens.getAccessToken(), carDto.getCarId())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(CarDto.class);

        assertCarData(carData, car);
    }

    @Test
    public void testGetMyCars() throws Exception {
        var personData = PersonFactory.getPerson();

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        List<CarDto> createdCars = CarSteps.createManyCars(tokens.getAccessToken(), CARS_COUNT_TO_TEST_MY_CARS);

        var myCars = CarApi.getMyCars(tokens.getAccessToken())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarDto.class);

        assertThat(myCars.size()).isEqualTo(createdCars.size());

        for (CarDto createdCar : createdCars) {
            var myCar = myCars.stream().filter(c -> Objects.equals(c.getVinNumber(), createdCar.getVinNumber().toUpperCase())).findFirst();

            if (myCar.isEmpty()) {
                throw new Exception("Cannot find result car by VIN");
            }

            assertCarData(createdCar, myCar.get());
        }
    }

    @Test
    public void testDeleteCar() {
        var personData = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        var tokens = PersonSteps.registerAndLogin(personData);

        createdPersonIds.add(tokens.getPerson().getPersonId());

        var carDto = CarSteps.createCar(tokens.getAccessToken());

        CarApi.deleteCar(tokens.getAccessToken(), carDto.getCarId())
                .then()
                .statusCode(204);
    }

    private static void assertCarData(CreateCarRequest createdCar, CarDto result) {
        assertThat(result.getVinNumber()).isEqualTo(createdCar.getVinNumber().toUpperCase());
        assertThat(result.getStateNumber()).isEqualTo((createdCar.getStateNumber() == null) ? null : createdCar.getStateNumber().toUpperCase());
        assertThat(result.getBrandName()).isEqualTo(createdCar.getBrandName());
        assertThat(result.getModelName()).isEqualTo(createdCar.getModelName());
        assertThat(result.getBodyName()).isEqualTo(createdCar.getBodyName());
        assertThat(result.getReleaseYear()).isEqualTo(createdCar.getReleaseYear());
        assertThat(result.getGearboxName()).isEqualTo(createdCar.getGearboxName());
        assertThat(result.getDriveName()).isEqualTo(createdCar.getDriveName());
        assertThat(result.getVehicleWeightKg()).isEqualTo(createdCar.getVehicleWeightKg());
        assertThat(result.getEnginePowerHp()).isEqualTo(createdCar.getEnginePowerHp());
        assertThat(result.getEnginePowerKw()).isEqualTo(createdCar.getEnginePowerKw());
        assertThat(result.getEngineCapacityL()).isEqualTo(createdCar.getEngineCapacityL());
        assertThat(result.getTankCapacityL()).isEqualTo(createdCar.getTankCapacityL());
        assertThat(result.getFuelTypeName()).isEqualTo(createdCar.getFuelTypeName());
    }

    private static void assertCarData(UpdateCarInfoRequest createdCar, CarDto result) {
        assertThat(result.getVinNumber()).isEqualTo(createdCar.getVinNumber().toUpperCase());
        assertThat(result.getStateNumber()).isEqualTo((createdCar.getStateNumber() == null) ? null : createdCar.getStateNumber().toUpperCase());
        assertThat(result.getBrandName()).isEqualTo(createdCar.getBrandName());
        assertThat(result.getModelName()).isEqualTo(createdCar.getModelName());
        assertThat(result.getBodyName()).isEqualTo(createdCar.getBodyName());
        assertThat(result.getReleaseYear()).isEqualTo(createdCar.getReleaseYear());
        assertThat(result.getGearboxName()).isEqualTo(createdCar.getGearboxName());
        assertThat(result.getDriveName()).isEqualTo(createdCar.getDriveName());
        assertThat(result.getVehicleWeightKg()).isEqualTo(createdCar.getVehicleWeightKg());
        assertThat(result.getEnginePowerHp()).isEqualTo(createdCar.getEnginePowerHp());
        assertThat(result.getEnginePowerKw()).isEqualTo(createdCar.getEnginePowerKw());
        assertThat(result.getEngineCapacityL()).isEqualTo(createdCar.getEngineCapacityL());
        assertThat(result.getTankCapacityL()).isEqualTo(createdCar.getTankCapacityL());
        assertThat(result.getFuelTypeName()).isEqualTo(createdCar.getFuelTypeName());
    }

    private static void assertCarData(CarDto createdCar, CarDto result) {
        assertThat(result.getVinNumber()).isEqualTo(createdCar.getVinNumber().toUpperCase());
        assertThat(result.getStateNumber()).isEqualTo((createdCar.getStateNumber() == null) ? null : createdCar.getStateNumber().toUpperCase());
        assertThat(result.getBrandName()).isEqualTo(createdCar.getBrandName());
        assertThat(result.getModelName()).isEqualTo(createdCar.getModelName());
        assertThat(result.getBodyName()).isEqualTo(createdCar.getBodyName());
        assertThat(result.getReleaseYear()).isEqualTo(createdCar.getReleaseYear());
        assertThat(result.getGearboxName()).isEqualTo(createdCar.getGearboxName());
        assertThat(result.getDriveName()).isEqualTo(createdCar.getDriveName());
        assertThat(result.getVehicleWeightKg()).isEqualTo(createdCar.getVehicleWeightKg());
        assertThat(result.getEnginePowerHp()).isEqualTo(createdCar.getEnginePowerHp());
        assertThat(result.getEnginePowerKw()).isEqualTo(createdCar.getEnginePowerKw());
        assertThat(result.getEngineCapacityL()).isEqualTo(createdCar.getEngineCapacityL());
        assertThat(result.getTankCapacityL()).isEqualTo(createdCar.getTankCapacityL());
        assertThat(result.getFuelTypeName()).isEqualTo(createdCar.getFuelTypeName());
    }

    @BeforeAll
    public static void setup() {
        var operator = PersonFactory.getPerson(PersonFactory.Role.OPERATOR);

        startOperatorTokens = PersonSteps.registerAndLogin(operator);
    }

    @AfterAll
    public static void cleanup() {
        createdPersonIds.add(startOperatorTokens.getPerson().getPersonId());

        createdCarsIds.forEach(id ->
                CarSteps.deleteCarById(startOperatorTokens.getAccessToken(), id)
        );

        createdPersonIds.forEach(id ->
                PersonSteps.deletePerson(startOperatorTokens.getAccessToken(), id)
        );
    }
}
