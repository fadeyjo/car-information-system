import { LoginResponse, LoginResponseDto } from "@renderer/types/auth.types";
import { mapPersonDto } from "./person.mapper";

export const mapLoginResponseDto = (
  dto: LoginResponseDto
): LoginResponse => {

  return {
    ...dto,
    person: mapPersonDto(dto.person)
  };
};