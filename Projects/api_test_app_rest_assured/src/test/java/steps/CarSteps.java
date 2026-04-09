package steps;

import api.CarApi;
import factories.CarFactory;
import io.restassured.http.ContentType;
import models.requests.CreateCarRequest;
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

        return CarApi.createCar(accessToken, carData)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .as(CarDto.class);
    }

    public static List<CarDto> createManyCars(String accessToken, Integer count) {
        List<CarDto> cars = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            cars.add(createCar(accessToken));
        }

        return cars;
    }
}
