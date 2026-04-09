package utils;

import api.DictionariesApi;
import io.restassured.http.ContentType;
import models.responses.*;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Random;

public class RandomCarDataUtil {

    public static String getRandomVinNumber(Faker faker) {
        return faker.regexify("[A-Za-z0-9]{17}");
    }
    
    public static String getRandomStateNumber(Faker faker) {
        var rand = new Random();

        var isNull = rand.nextBoolean();

        if (isNull) {
            return null;
        }

        return faker.regexify("[авекмнорстухАВЕКМНОРСТУХ]{1}") +
                faker.number().digits(3) +
                faker.regexify("[авекмнорстухАВЕКМНОРСТУХ]{2}") +
                faker.regexify("[0-9]{2,3}");
    }
    
    public static String getRandomBrandName() {
        var brands = DictionariesApi.getAllCarBrands()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarBrandDto.class);

        var rand = new Random();
        var maxBound = brands.size() - 1;
        var index = rand.nextInt(maxBound + 1);

        return brands.get(index).getBrandName();
    }
    
    public static String getRandomModelName(String brandName) {
        var models = DictionariesApi.getAllCarModels(brandName)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarModelDto.class);

        var rand = new Random();
        var maxBound = models.size() - 1;
        var index = rand.nextInt(maxBound + 1);

        return models.get(index).getModelName();
    }
    
    public static String getRandomBodyName() {
        var bodies = DictionariesApi.getAllCarBodies()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarBodyDto.class);

        var rand = new Random();
        var maxBound = bodies.size() - 1;
        var index = rand.nextInt(maxBound + 1);

        return bodies.get(index).getBodyName();
    }
    
    public static Integer getRandomReleaseYear(Faker faker, Integer minYear, Integer maxYear) {
        return faker.number().numberBetween(minYear, maxYear);
    }

    public static Integer getRandomReleaseYear(Faker faker) {
        return getRandomReleaseYear(faker, 2000, LocalDate.now().getYear());
    }
    
    public static String getRandomGearboxName() {
        var gearboxes = DictionariesApi.getAllCarGearboxes()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarGearboxDto.class);

        var rand = new Random();
        var maxBound = gearboxes.size() - 1;
        var index = rand.nextInt(maxBound + 1);

        return gearboxes.get(index).getGearboxName();
    }
    
    public static String getRandomDriveName() {
        var drives = DictionariesApi.getAllCarDrives()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CarDriveDto.class);

        var rand = new Random();
        var maxBound = drives.size() - 1;
        var index = rand.nextInt(maxBound + 1);

        return drives.get(index).getDriveName();
    }
    
    public static Integer getRandomVehicleWeightKg(Faker faker, Integer minWeight, Integer maxWeight) {
        return faker.number().numberBetween(minWeight, maxWeight);
    }

    public static Integer getRandomVehicleWeightKg(Faker faker) {
        return getRandomVehicleWeightKg(faker, 750, 5000);
    }
    
    public static Integer getRandomEnginePowerHp(Faker faker, Integer minPowerHp, Integer maxPowerHp) {
        return faker.number().numberBetween(minPowerHp, maxPowerHp);
    }

    public static Integer getRandomEnginePowerHp(Faker faker) {
        return getRandomEnginePowerHp(faker, 60, 5000);
    }
    
    public static Double getRandomEnginePowerKw(Faker faker, Integer precision, Integer minPowerKw, Integer maxPowerKw) {
        return faker.number().randomDouble(precision, minPowerKw, maxPowerKw);
    }

    public static Double getRandomEnginePowerKw(Faker faker) {
        return getRandomEnginePowerKw(faker, 1, 2, 60);
    }

    public static Double getRandomEnginePowerKw(Faker faker, Integer minPowerKw, Integer maxPowerKw) {
        return getRandomEnginePowerKw(faker, 1, minPowerKw, maxPowerKw);
    }
    
    public static Double getRandomEngineCapacityL(Faker faker, Integer precision, Integer minCap, Integer maxCap) {
        return faker.number().randomDouble(precision, minCap, maxCap);
    }

    public static Double getRandomEngineCapacityL(Faker faker) {
        return getRandomEngineCapacityL(faker, 1, 1, 50);
    }

    public static Double getRandomEngineCapacityL(Faker faker, Integer minCap, Integer maxCap) {
        return faker.number().randomDouble(1, minCap, maxCap);
    }
    
    public static Integer getRandomTankCapacityL(Faker faker, Integer minCap, Integer maxCap) {
        return faker.number().numberBetween(minCap, maxCap);
    }

    public static Integer getRandomTankCapacityL(Faker faker) {
        return getRandomTankCapacityL(faker, 10, 250);
    }
    
    public static String getRandomFuelTypeName() {
        var fuelTypes = DictionariesApi.getAllFuelTypes()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", FuelTypeDto.class);

        var rand = new Random();
        var maxBound = fuelTypes.size() - 1;
        var index = rand.nextInt(maxBound + 1);

        return fuelTypes.get(index).getTypeName();
    }
    
}
