import { EmailCodePurpose } from "./EmailCodePurpose";

// 인증코드 발송
export interface SendCodeRequest {
  email: string;
  username?: string; // RESET_PASSWORD에서만 필요
  purpose: EmailCodePurpose;
}

// 인증코드 검증
export interface VerifyCodeRequest {
  email: string;
  code: string;
  purpose: EmailCodePurpose;
}