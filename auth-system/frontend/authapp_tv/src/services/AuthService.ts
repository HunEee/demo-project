import apiClient from "@/config/apiClient";
import authClient from "@/config/authClient";
import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";

const normalizeLoginResponse = (data: LoginResponseData | string): LoginResponseData => {
  if (typeof data === "string") {
    return JSON.parse(data) as LoginResponseData;
  }

  return data;
};

// =============================
// 로그인
// =============================
export const loginUser = async (loginData: LoginData) => {
  // 로그인 요청 → accessToken + user 정보 반환
  const response = await authClient.post<LoginResponseData | string>("/auth/login", loginData);
  return normalizeLoginResponse(response.data);
};

// =============================
// 로그아웃
// =============================
export const logoutUser = async () => {
  // 서버에 로그아웃 요청 (쿠키/세션 제거 등)
  const response = await authClient.post(`/auth/logout`);
  return response.data;
};

// =============================
// 현재 로그인 사용자 조회
// =============================
export const getCurrentUser = async () => {
  const response = await apiClient.get<User>("/users/me");
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
export type RefreshReason = "ACCESS_TOKEN_MISSING" | "ACCESS_TOKEN_EXPIRED" | "UNKNOWN";

export const refreshToken = async (reason: RefreshReason = "UNKNOWN") => {
  // accessToken 만료 시 새로운 토큰 발급 요청
  const response = await authClient.post<LoginResponseData>(
    `/jwt/refresh`,
    undefined,
    {
      headers: {
        "X-Refresh-Reason": reason,
      },
    }
  );
  return response.data;
};

// =============================
// OAUth 쿠키 처리(Refresh Token으로 Access토큰 발급)
// =============================
export const exchangeOAuthCookie = async () => {
  const response = await authClient.post<LoginResponseData>(`/jwt/exchange`);
  return response.data;
};

