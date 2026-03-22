from dataclasses import dataclass

@dataclass
class SignUpRequest:
    email: str
    phone: str
    last_name: str
    first_name: str
    birth: str
    password: str
    role_id: int
    patronymic: str | None = None
    drive_license: str | None = None

    def to_dict(self) -> dict:
        new_dict = dict()

        new_dict["email"] = self.email
        new_dict["phone"] = self.phone
        new_dict["lastName"] = self.last_name
        new_dict["firstName"] = self.first_name
        new_dict["birth"] = self.birth
        new_dict["password"] = self.password
        new_dict["roleId"] = self.role_id
        new_dict["patronymic"] = self.patronymic
        new_dict["driveLicense"] = self.drive_license

        return new_dict


