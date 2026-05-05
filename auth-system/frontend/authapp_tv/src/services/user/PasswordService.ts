import authClient from "@/config/authClient";
import apiClient from "@/config/apiClient";
import type {ChangePasswordRequest, ResetPasswordRequest,} from "@/models/user/PasswordModels";

// =============================
// 비밀번호 관련 API
// =============================

// 비밀번호 변경 (로그인)
export const changePassword = async (data: ChangePasswordRequest) => {
  await apiClient.put("/users/me/password", data);
};

// 비밀번호 재설정 (로그아웃)
export const resetPassword = async (data: ResetPasswordRequest) => {
  const res = await authClient.post("/users/password/reset", data);
  return res.data;
};