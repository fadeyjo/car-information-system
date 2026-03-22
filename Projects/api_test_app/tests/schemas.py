from pydantic import BaseModel, Field
from datetime import date

class PersonDto(BaseModel):
    personId: int = Field(alias="personId")
    email: str
    phone: str
    last_name: str = Field(alias="lastName")
    first_name: str = Field(alias="firstName")
    patronymic: str | None = None
    birth: date
    drive_license: str | None = Field(alias="driveLicense")
    role_id: int = Field(alias="roleId")
    avatar_id: int = Field(alias="avatarId")

class TokensDto(BaseModel):
    access_token: str = Field(alias="accessToken")
    refresh_token: str = Field(alias="refreshToken")
