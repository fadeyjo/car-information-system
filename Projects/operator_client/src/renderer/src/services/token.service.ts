import { Person } from "@renderer/types/person.types";

export const tokenService = {
  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  },

  setAccessToken(token: string): void {
    localStorage.setItem('accessToken', token);
  },

  removeAccessToken(): void {
    localStorage.removeItem('accessToken');
  },

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  },

  setRefreshToken(token: string): void {
    localStorage.setItem('refreshToken', token);
  },

  removeRefreshToken(): void {
    localStorage.removeItem('refreshToken');
  },

  clear(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }
};