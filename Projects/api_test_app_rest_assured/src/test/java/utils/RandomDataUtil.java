package utils;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDataUtil {
    public static LocalDate getRandomDateByMinAge(Integer min) {
        LocalDate now = LocalDate.now();

        LocalDate maxDate = now.minusYears(min);

        LocalDate minDate = now.minusYears(100);

        long minDay = minDate.toEpochDay();
        long maxDay = maxDate.toEpochDay();

        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay + 1);

        return LocalDate.ofEpochDay(randomDay);
    }

    public static LocalDate getRandomBirthByMinAge() {
        return getRandomDateByMinAge(18);
    }

    public static String getRandomStringWithLength(Integer length) {
        String random;

        do {
            random = UUID.randomUUID().toString().replace("-", "");
        } while (random.length() < length);

        return random.substring(0, length);
    }

    public static String getRandomStringWithLength() {
        return getRandomStringWithLength(8);
    }

    public static String getRandomPhone() {
        StringBuilder phone = new StringBuilder("+7");

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            phone.append(rand.nextInt(10));
        }

        return phone.toString();
    }

    public static String getRandomCapString(Integer length) {
        String random;

        do {
            random = UUID.randomUUID().toString().replace("-", "");
        } while (random.length() < length);

        random = random.substring(0, length).toLowerCase();

        return random.substring(0, 1).toUpperCase() + random.substring(1);
    }

    public static String getRandomCapString() {
        return getRandomCapString(8);
    }

    public static String getRandomCapStringOrNull(Integer length) {
        Random rand = new Random();

        boolean isNull = rand.nextInt() % 2 == 0;

        if (isNull) {
            return null;
        }

        return getRandomCapString(length);
    }

    public static String getRandomCapStringOrNull() {
        return getRandomCapStringOrNull(8);
    }

    public static String getRandomDriveLicense() {
        StringBuilder driveLicense = new StringBuilder();

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            driveLicense.append(rand.nextInt(10));
        }

        return driveLicense.toString();
    }
}
