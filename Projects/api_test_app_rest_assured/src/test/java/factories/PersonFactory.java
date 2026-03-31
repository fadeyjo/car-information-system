package factories;

import models.requests.SignInRequest;
import models.requests.SignUpRequest;
import models.requests.UpdatePersonInfoRequest;
import utils.RandomDataUtil;

import java.time.LocalDate;

public class PersonFactory {

    public enum InvalidRegisterUserScenario {
        EMAIL,
        PHONE,
        DRIVE_LICENSE,
        PASSWORD_TOO_SHORT,
        EMPTY_FIRST_NAME,
        BIRTH_IN_FUTURE,
        NON_EXISTENT_ROLE_ID
    }

    public enum InvalidUpdateUserScenario {
        EMAIL,
        PHONE,
        DRIVE_LICENSE,
        PASSWORD_TOO_SHORT,
        EMPTY_FIRST_NAME,
        BIRTH_IN_FUTURE
    }

    public static SignUpRequest getUser() {
        String email = RandomDataUtil.getRandomStringWithLength() + "@gmail.com";
        String phone = RandomDataUtil.getRandomPhone();
        String lastName = RandomDataUtil.getRandomCapString();
        String firstName = RandomDataUtil.getRandomCapString();
        String patronymic = RandomDataUtil.getRandomCapStringOrNull();
        String password = RandomDataUtil.getRandomStringWithLength(16);
        String driveLicense = RandomDataUtil.getRandomDriveLicense();
        LocalDate birth = RandomDataUtil.getRandomBirthByMinAge();

        return SignUpRequest
                .builder()
                .email(email)
                .phone(phone)
                .lastName(lastName)
                .firstName(firstName)
                .patronymic(patronymic)
                .password(password)
                .driveLicense(driveLicense)
                .birth(birth)
                .roleId(1)
                .build();
    }

    public static SignUpRequest getOperator() {
        String email = RandomDataUtil.getRandomStringWithLength() + "@gmail.com";
        String phone = RandomDataUtil.getRandomPhone();
        String lastName = RandomDataUtil.getRandomCapString();
        String firstName = RandomDataUtil.getRandomCapString();
        String patronymic = RandomDataUtil.getRandomCapStringOrNull();
        String password = RandomDataUtil.getRandomStringWithLength(16);
        String driveLicense = RandomDataUtil.getRandomDriveLicense();
        LocalDate birth = RandomDataUtil.getRandomBirthByMinAge();

        return SignUpRequest
                .builder()
                .email(email)
                .phone(phone)
                .lastName(lastName)
                .firstName(firstName)
                .patronymic(patronymic)
                .password(password)
                .driveLicense(driveLicense)
                .birth(birth)
                .roleId(2)
                .build();
    }

    public static UpdatePersonInfoRequest getUpdatedData() {
        String email = RandomDataUtil.getRandomStringWithLength() + "@gmail.com";
        String phone = RandomDataUtil.getRandomPhone();
        String lastName = RandomDataUtil.getRandomCapString();
        String firstName = RandomDataUtil.getRandomCapString();
        String patronymic = RandomDataUtil.getRandomCapStringOrNull();
        String driveLicense = RandomDataUtil.getRandomDriveLicense();
        LocalDate birth = RandomDataUtil.getRandomBirthByMinAge();

        return UpdatePersonInfoRequest
                .builder()
                .email(email)
                .phone(phone)
                .lastName(lastName)
                .firstName(firstName)
                .patronymic(patronymic)
                .driveLicense(driveLicense)
                .birth(birth)
                .build();
    }

    public static SignUpRequest getInvalidUser(InvalidRegisterUserScenario s) {
        var user = getUser();

        switch (s) {
            case EMAIL -> {
                user.setEmail("invalid");
                return user;
            }

            case PHONE -> {
                user.setPhone("+7123567665");
                return user;
            }

            case DRIVE_LICENSE -> {
                user.setDriveLicense("123456789");
                return user;
            }

            case BIRTH_IN_FUTURE -> {
                user.setBirth(LocalDate.of(LocalDate.now().getYear() + 2, 5, 15));
                return user;
            }

            case EMPTY_FIRST_NAME -> {
                user.setFirstName(null);
                return user;
            }

            case NON_EXISTENT_ROLE_ID -> {
                user.setRoleId(3);
                return user;
            }

            case PASSWORD_TOO_SHORT -> {
                user.setPhone("1234567");
                return user;
            }

            default -> throw new IllegalArgumentException("Unknown scenario");
        }
    }

    public static UpdatePersonInfoRequest getInvalidUpdatedData(InvalidUpdateUserScenario s) {
        var user = getUpdatedData();

        switch (s) {
            case EMAIL -> {
                user.setEmail("invalid");
                return user;
            }

            case PHONE -> {
                user.setPhone("+7123567665");
                return user;
            }

            case DRIVE_LICENSE -> {
                user.setDriveLicense("123456789");
                return user;
            }

            case BIRTH_IN_FUTURE -> {
                user.setBirth(LocalDate.of(LocalDate.now().getYear() + 2, 5, 15));
                return user;
            }

            case EMPTY_FIRST_NAME -> {
                user.setFirstName(null);
                return user;
            }

            case PASSWORD_TOO_SHORT -> {
                user.setPhone("1234567");
                return user;
            }

            default -> throw new IllegalArgumentException("Unknown scenario");
        }
    }
}
