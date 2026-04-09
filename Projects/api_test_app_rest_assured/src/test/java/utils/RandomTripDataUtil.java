package utils;

import net.datafaker.Faker;

public class RandomTripDataUtil {
    public static String getMacAddress() {
        var faker = new Faker();

        return faker.regexify("([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}");
    }
}
