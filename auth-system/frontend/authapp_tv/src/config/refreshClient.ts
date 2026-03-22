import axios from "axios";

export const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // 쿠키 refresh token 사용 시 필수
  headers: {
    "Content-Type": "application/json",
  },
});