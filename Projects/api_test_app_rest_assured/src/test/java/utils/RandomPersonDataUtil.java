package utils;

import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Random;

public class RandomPersonDataUtil {

    public static String getRandomEmail(Faker faker) {
        return faker.internet().emailAddress();
    }

    public static String getRandomPhone(Faker faker) {
        return "+7" + faker.number().digits(10);
    }

    public static String getRandomLastName(Faker faker) {
        return faker.name().lastName();
    }

    public static String getRandomFirstName(Faker faker) {
        return faker.name().firstName();
    }

    public static String getRandomPatronymic(Faker faker) {
        var rand = new Random();

        var isNull = rand.nextBoolean();

        if (isNull) {
            return null;
        }

        return faker.name().lastName();
    }

    public static String getRandomPassword(Faker faker, Integer minLength, Integer maxLength) {
        return faker.credentials().password(minLength, maxLength);
    }

    public static String getRandomPassword(Faker faker) {
        return getRandomPassword(faker, 8, 32);
    }

    public static String getRandomDriveLicense(Faker faker) {
        return faker.number().digits(10);
    }

    public static LocalDate getRandomBirth(Faker faker, Integer minAge, Integer maxAge) {
        return faker.timeAndDate().birthday(minAge, maxAge);
    }

    public static LocalDate getRandomBirth(Faker faker) {
        return getRandomBirth(faker, 18, 80);
    }
}
