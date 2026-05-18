import { Person, PersonDto } from "@renderer/types/person.types";

export const mapPersonDto = (
  dto: PersonDto
): Person => {
  return {
    ...dto,
    birth: new Date(dto.birth)
  };
};