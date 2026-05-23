import authClient from "@/config/authClient";
import apiClient from "@/config/apiClient";

import type {
  SignupRequest,
  CheckUsernameRequest,
  UserResponse,
  UpdateUserRequest,
  FindUsernameRequest,
} from "@/models/user/UserModels";
import type {
  ChangePasswordRequest,
  ResetPasswordRequest,
} from "@/models/user/PasswordModels";


// =============================
// 회원가입
// =============================
export const signup = async (data: SignupRequest) => {
  const res = await authClient.post("/users", data);
  return res.data; // Long (userId)
};


// =============================
// username 중복 체크
// =============================
export const checkUsername = async (data: CheckUsernameRequest) => {
  const res = await authClient.post("/users/exists", data);
  return res.data as boolean;
};


// =============================
// 내 정보 조회
// =============================
export const getMyInfo = async () => {
  const res = await apiClient.get("/users/me");
  return res.data as UserResponse;
};


// =============================
// 유저 수정
// =============================
export const updateUser = async (data: UpdateUserRequest) => {
  const res = await apiClient.patch("/users/me", data);
  return res.data; // Long
};


// =============================
// 회원 탈퇴
// =============================
export const deleteUser = async () => {
  await apiClient.delete("/users/me");
};


// =============================
// 아이디 찾기
// =============================
export const findUsername = async (data: FindUsernameRequest) => {
  const res = await authClient.post("/users/find-username", data);
  return res.data.username;
};


// =============================
// 비밀번호 변경 (로그인)
// =============================
export const changePassword = async (data: ChangePasswordRequest) => {
  await apiClient.put("/users/me/password", data);
};


// =============================
// 비밀번호 재설정 (로그아웃)
// =============================
export const resetPassword = async (data: ResetPasswordRequest) => {
  const res = await authClient.post("/users/password/reset", data);
  return res.data;
};
