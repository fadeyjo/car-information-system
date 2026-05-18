import api from "./axios";
import { Person, PersonDto } from "@renderer/types/person.types";
import { mapPersonDto } from "@renderer/mappers/person.mapper";

export const personApi = {

  async getPersonData(): Promise<Person> {

    const response =
      await api.get<PersonDto>(
        '/persons'
      );
    
      console.log(response.data)

    return mapPersonDto(
      response.data
    );
  },

  async getAvatar(avatarId: number) {
    const response = await api.get(
      `/avatars/${avatarId}`,
      {
        responseType: "blob",
      }
    );

    console.log(response.data instanceof Blob);

    return URL.createObjectURL(response.data);
  }
};