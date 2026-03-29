import apiClient from "@/config/apiClient";
import authClient from "@/config/authClient";
import type RegisterData from "@/models/RegisterData";
import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";


// =============================
// 회원가입
// =============================
export const registerUser = async (signupData: RegisterData) => {
  // 서버에 회원가입 데이터 전송
  const response = await authClient.post("/user", signupData);
  return response.data;
};

// =============================
// 로그인
// =============================
export const loginUser = async (loginData: LoginData) => {
  // 로그인 요청 → accessToken + user 정보 반환
  const response = await authClient.post<LoginResponseData>("/login",loginData);
  return response.data;
};

// =============================
// 로그아웃
// =============================
export const logoutUser = async () => {
  // 서버에 로그아웃 요청 (쿠키/세션 제거 등)
  const response = await authClient.post(`/logout`);
  return response.data;
};

// =============================
// 현재 로그인 사용자 조회
// =============================
export const getCurrentUser = async () => {
  const response = await apiClient.get<User>(`/user`);
  return response.data;
};

export const getCurrentUserByEmail = async (emailId: string | undefined) => {
  // 이메일 기반 사용자 조회
  const response = await apiClient.get<User>(`/users/email/${emailId}`);
  return response.data;
};


// =============================
// 토큰 재발급 (Refresh Token)
// =============================
export const refreshToken = async () => {
  // accessToken 만료 시 새로운 토큰 발급 요청
  const response = await authClient.post<LoginResponseData>(`/jwt/refresh`);
  return response.data;
};

