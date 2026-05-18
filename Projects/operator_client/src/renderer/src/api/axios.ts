import axios from 'axios';
import { tokenService } from '../services/token.service';

const API_URL = import.meta.env.VITE_API_URL;

export const api = axios.create({
  baseURL: API_URL,
  withCredentials: true
});

api.interceptors.request.use((config) => {
  const token = tokenService.getAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,

  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;

      try {
        const refreshToken =
          tokenService.getRefreshToken();

        if (!refreshToken) {
          throw new Error('Refresh token missing');
        }

        const response = await axios.post(
          API_URL + '/refresh-tokens/refresh',
          {
            refreshToken
          },
          {
            withCredentials: true
          }
        );

        const {
          accessToken,
          refreshToken: newRefreshToken
        } = response.data;

        tokenService.setAccessToken(accessToken);

        if (newRefreshToken) {
          tokenService.setRefreshToken(
            newRefreshToken
          );
        }

        originalRequest.headers.Authorization =
          `Bearer ${accessToken}`;

        return api(originalRequest);

      } catch (refreshError) {

        tokenService.clear();

        window.location.href = '/login';

        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;