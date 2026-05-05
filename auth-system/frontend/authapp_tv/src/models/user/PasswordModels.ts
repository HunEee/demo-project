// =============================
// 비밀번호 변경 (로그인 상태)
// =============================
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// =============================
// 비밀번호 재설정 (로그아웃)
// =============================
export interface ResetPasswordRequest {
  username: string;
  email: string;
  verificationCode: string;
  newPassword: string;
}