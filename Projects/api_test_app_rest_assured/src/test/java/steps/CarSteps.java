package steps;

import api.CarApi;
import factories.CarFactory;
import io.restassured.http.ContentType;
import models.requests.CreateCarRequest;
import models.requests.UpdateCarInfoRequest;
import models.responses.CarDto;

import java.util.ArrayList;
import java.util.List;

public class CarSteps {

    public static CarDto createCar(String accessToken, CreateCarRequest carData) {
        return CarApi.createCar(accessToken, carData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(CarDto.class);
    }

    public static CarDto createCar(String accessToken) {
        var carData = CarFactory.getCar();

        return createCar(accessToken, carData);
    }

    public static List<CarDto> createManyCars(String accessToken, Integer count) {
        List<CarDto> cars = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            cars.add(createCar(accessToken));
        }

        return cars;
    }

    public static void updateCar(String accessToken, Integer carId, UpdateCarInfoRequest newCarData) {
        CarApi.updateCar(accessToken, newCarData, carId)
                .then()
                .statusCode(204);
    }

    public static void updateCar(String accessToken, Integer carId) {
        var newCarData = CarFactory.getUpdateCarData();

        updateCar(accessToken, carId, newCarData);
    }

    public static CarDto getCarDataById(String accessToken, Integer carId) {
        return CarApi.getCarById(accessToken, carId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(CarDto.class);
    }

    public static List<CarDto> getMyCars(String accessToken) {
        return CarApi.getMyCars(accessToken)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarDto.class);
    }

    public static void deleteCarById(String accessToken, Integer carId) {
        CarApi.deleteCar(accessToken, carId)
                .then()
                .statusCode(204);
    }
}
