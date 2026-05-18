export const useAuth = () => {
  const accessToken = localStorage.getItem("accessToken");
  const refreshToken = localStorage.getItem("refreshToken");

  return {
    isAuthenticated: accessToken && refreshToken,
  };
};