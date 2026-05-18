import { LoginRequest, LoginResponse, LoginResponseDto, LogOutDto } from "@renderer/types/auth.types";
import api from "./axios";
import { mapLoginResponseDto } from "@renderer/mappers/auth.mapper";

export const authApi = {

  async login(
    data: LoginRequest
  ): Promise<LoginResponse> {

    const response =
      await api.post<LoginResponseDto>(
        '/refresh-tokens/login',
        data
      );

    return mapLoginResponseDto(
      response.data
    );
  },

  async logout(): Promise<LogOutDto> {

    const response =
      await api.post<LogOutDto>(
        '/refresh-tokens/logout'
      );

    return response.data;
  }
};