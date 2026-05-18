import { Person, PersonDto } from "./person.types";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponseDto {
  person: PersonDto;
  accessToken: string;
  refreshToken: string;
}

export interface LoginResponse {
  person: Person;
  accessToken: string;
  refreshToken: string;
}

export interface LogOutDto {
  personId: number;
}