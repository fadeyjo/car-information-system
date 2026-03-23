import httpx
import tests.request_bodies as e
from tests.schemas import PersonDto
import pytest

class TestPerson:
    def test_person_registration(self, client: httpx.Client, operator_tokens: dict, body_to_sign_up: e.SignUpRequest):
        person_json = body_to_sign_up.to_dict()

        response = client.post("/persons", json=person_json)
        assert response.status_code == 201, "Wrong to registrate"
        new_person_data = response.json()
        PersonDto.model_validate(new_person_data)
        new_person_id = int(new_person_data["personId"])

        access_token = operator_tokens["access_token"]
        headers = {"Authorization": f"Bearer {access_token}"}

        delete_response = client.delete(f"/persons/{new_person_id}", headers=headers)
        assert delete_response.status_code == 204, "Wrong deleting"
    
    @pytest.mark.parametrize(
            "email, phone, birth, password, drive_license, role_id",
            [
                ("", "+71237236611", "2001-06-08", "12345678", "1234567890", 1),
                (None, "+71237236611", "2001-06-08", "12345678", "1234567890", 1),
                ("validgmail.com", "+71237236611", "2001-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", "", "2001-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", None, "2001-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", "not a number", "2001-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", "+7123", "2001-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", "+7123723661190", "2001-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", "+71237236611", "", "12345678", "1234567890", 1),
                ("valid@gmail.com", "+71237236611", None, "12345678", "1234567890", 1),
                ("valid@gmail.com", "+71237236611", "20012-06-08", "12345678", "1234567890", 1),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "312", "1234567890", 1),
                ("valid@gmail.com", "+71237236611", "2001-06-08", None, "1234567890", 1),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "8"*33, "1234567890", 1),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "12345678", "12345678901", 1),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "12345678", "123456789", 1),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "12345678", "1234567890", None),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "12345678", "1234567890", ""),
                ("valid@gmail.com", "+71237236611", "2001-06-08", "12345678", "1234567890", "string role")
            ]
    )
    def test_person_with_uncorrect_data(self, email, phone, birth, password, drive_license, role_id, client: httpx.Client, body_to_sign_up: e.SignUpRequest):
        body_to_sign_up.email = email
        body_to_sign_up.phone = phone
        body_to_sign_up.birth = birth
        body_to_sign_up.password = password
        body_to_sign_up.drive_license = drive_license
        body_to_sign_up.role_id = role_id

        person_json = body_to_sign_up.to_dict()

        response = client.post("/persons", json=person_json)
        assert response.status_code == 400, "Must be wrong to registrate"
    
    def test_registration_with_duble(self, client: httpx.Client, registered_user, random_email, random_phone):
        new_person = e.SignUpRequest(
            registered_user["email"], random_phone,
            "NewLastName", "NewFirstName",
            "2001-06-09", "12345678",
            1, "NewPersonPatronymic",
            "1234561923"
        )

        response = client.post("/persons", json=new_person.to_dict())
        assert response.status_code == 409

        new_person.email = random_email
        new_person.phone = registered_user["phone"]
        response = client.post("/persons", json=new_person.to_dict())
        assert response.status_code == 409

        if registered_user["driveLicense"] == None:
            return

        new_person.phone = random_phone
        new_person.drive_license = registered_user["driveLicense"]
        response = client.post("/persons", json=new_person.to_dict())
        assert response.status_code == 409
