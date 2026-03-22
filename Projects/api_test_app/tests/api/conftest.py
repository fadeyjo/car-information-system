import pytest
import httpx
from configuration import BASE_URL
import tests.request_bodies as e
import random
import string
from datetime import date
from dateutil.relativedelta import relativedelta
from typing import Iterator
from tests.schemas import TokensDto
from tests.schemas import PersonDto

@pytest.fixture(scope="session")
def client() -> Iterator[httpx.Client]:
    with httpx.Client(base_url=BASE_URL, verify=False, timeout=10) as c:
        yield c

@pytest.fixture
def random_email() -> str:
    characters = string.ascii_letters
    random_part = ''.join(random.choices(characters, k=5))
    email = f"pytest_{random_part}@gmail.com"
    return email

@pytest.fixture
def random_phone() -> str:
    characters = string.digits
    random_part = ''.join(random.choices(characters, k=10))
    phone = f"+7{random_part}"
    return phone

@pytest.fixture
def random_drive_license() -> str:
    random_digit = random.randint(1, 10000)

    if (random_digit % 2 == 0):
        return None
    
    characters = string.digits
    drive_license = ''.join(random.choices(characters, k=10))
    return drive_license

@pytest.fixture
def random_password():
    characters = string.ascii_letters + string.digits
    return ''.join(random.choices(characters, k=17))

@pytest.fixture
def body_to_sign_up(random_email, random_phone, random_password, random_drive_license):
    number = random.randint(1, 10000)
    
    patronymic = None if number % 2 == 0 else "PytestPatronymic"

    return e.SignUpRequest(
        random_email, random_phone,
        "PytestLastName", "PytestFirstName",
        (date.today() - relativedelta(years=18, days=1)).isoformat(),
        random_password, 1,
        patronymic, random_drive_license
    )

@pytest.fixture(scope="session")
def authorized_operator(client: httpx.Client):
    body = dict()
    body["email"] = "test@mail.com"
    body["password"] = "12345678"

    response = client.post("/refresh-tokens/login", json=body)
    assert response.status_code == 200, "Must be authorized"
    TokensDto.model_validate(response.json())

    response_data = response.json()

    tokens = dict()
    tokens["access_token"] = response_data["accessToken"]
    tokens["refresh_token"] = response_data["refreshToken"]

    return tokens

@pytest.fixture
def registered_user(client: httpx.Client, authorized_operator: dict, body_to_sign_up: e.SignUpRequest):
        person_json = body_to_sign_up.to_dict()

        response = client.post("/persons", json=person_json)
        assert response.status_code == 201, "Wrong to registrate"
        new_person_data = response.json()
        PersonDto.model_validate(new_person_data)
        new_person_id = int(new_person_data["personId"])

        yield new_person_data

        access_token = authorized_operator["access_token"]
        headers = {"Authorization": f"Bearer {access_token}"}

        delete_response = client.delete(f"/persons/{new_person_id}", headers=headers)
        assert delete_response.status_code == 204, "Wrong deleting"

@pytest.fixture
def authorize_fun(client: httpx.Client):
    def authorize_person(email, password):
        auth_dict = dict()
        auth_dict["email"] = email
        auth_dict["password"] = password

        response = client.post("/refresh-tokens/login", json=auth_dict)

        assert response.status_code == 200

        tokens_data = response.json()

        PersonDto.model_validate(tokens_data)

        tokens_dict = dict()
        tokens_dict["access_token"] = tokens_data["accessToken"]
        tokens_dict["refresh_token"] = tokens_data["refreshToken"]

        return tokens_dict
    
    return authorize_person
