package factories;

import lombok.Getter;
import models.requests.SignUpRequest;
import models.requests.UpdatePersonInfoRequest;
import net.datafaker.Faker;
import utils.RandomPersonDataUtil;

import java.time.LocalDate;

public class PersonFactory {

    @Getter
    public enum Role {
        USER(1),
        OPERATOR(2);

        private final Integer roleId;

        Role(Integer roleId) {
            this.roleId = roleId;
        }
    }

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

    public static SignUpRequest getPerson(Role role) {
        var faker = new Faker();

        String email = RandomPersonDataUtil.getRandomEmail(faker);
        String phone = RandomPersonDataUtil.getRandomPhone(faker);
        String lastName = RandomPersonDataUtil.getRandomLastName(faker);
        String firstName = RandomPersonDataUtil.getRandomFirstName(faker);
        String patronymic = RandomPersonDataUtil.getRandomPatronymic(faker);
        String password = RandomPersonDataUtil.getRandomPassword(faker);
        String driveLicense = RandomPersonDataUtil.getRandomDriveLicense(faker);
        LocalDate birth = RandomPersonDataUtil.getRandomBirth(faker);

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
                .roleId(role.getRoleId())
                .build();
    }

    public static SignUpRequest getPerson() {
        var faker = new Faker();

        String email = RandomPersonDataUtil.getRandomEmail(faker);
        String phone = RandomPersonDataUtil.getRandomPhone(faker);
        String lastName = RandomPersonDataUtil.getRandomLastName(faker);
        String firstName = RandomPersonDataUtil.getRandomFirstName(faker);
        String patronymic = RandomPersonDataUtil.getRandomPatronymic(faker);
        String password = RandomPersonDataUtil.getRandomPassword(faker);
        String driveLicense = RandomPersonDataUtil.getRandomDriveLicense(faker);
        LocalDate birth = RandomPersonDataUtil.getRandomBirth(faker);

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
                .roleId(Role.USER.getRoleId())
                .build();
    }

    public static UpdatePersonInfoRequest getUpdatedData() {
        var faker = new Faker();

        String email = RandomPersonDataUtil.getRandomEmail(faker);
        String phone = RandomPersonDataUtil.getRandomPhone(faker);
        String lastName = RandomPersonDataUtil.getRandomLastName(faker);
        String firstName = RandomPersonDataUtil.getRandomFirstName(faker);
        String patronymic = RandomPersonDataUtil.getRandomPatronymic(faker);
        String driveLicense = RandomPersonDataUtil.getRandomDriveLicense(faker);
        LocalDate birth = RandomPersonDataUtil.getRandomBirth(faker);

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
        var user = getPerson();

        switch (s) {
            case EMAIL -> user.setEmail("invalid");

            case PHONE -> user.setPhone("+7123567665");

            case DRIVE_LICENSE -> user.setDriveLicense("123456789");

            case BIRTH_IN_FUTURE -> user.setBirth(LocalDate.of(LocalDate.now().getYear() + 2, 5, 15));

            case EMPTY_FIRST_NAME -> user.setFirstName(null);

            case NON_EXISTENT_ROLE_ID -> user.setRoleId(3);

            case PASSWORD_TOO_SHORT -> user.setPhone("1234567");

            default -> throw new IllegalArgumentException("Unknown scenario");
        }

        return user;
    }

    public static UpdatePersonInfoRequest getInvalidUpdatedData(InvalidUpdateUserScenario s) {
        var user = getUpdatedData();

        switch (s) {
            case EMAIL -> user.setEmail("invalid");

            case PHONE -> user.setPhone("+7123567665");

            case DRIVE_LICENSE -> user.setDriveLicense("123456789");

            case BIRTH_IN_FUTURE -> user.setBirth(LocalDate.of(LocalDate.now().getYear() + 2, 5, 15));

            case EMPTY_FIRST_NAME -> user.setFirstName(null);

            case PASSWORD_TOO_SHORT -> user.setPhone("1234567");

            default -> throw new IllegalArgumentException("Unknown scenario");
        }

        return user;
    }
}
