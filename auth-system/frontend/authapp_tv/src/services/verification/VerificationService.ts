import authClient from "@/config/authClient";
import type {SendCodeRequest, VerifyCodeRequest, } from "@/models/verification/VerificationModels";

// =============================
// 인증코드 관련 API
// =============================

// 인증코드 발송
export const sendEmailCode = async (data: SendCodeRequest) => {
  const res = await authClient.post("/verification/codes", data);
  return res.data;
};

// 인증코드 검증
export const verifyEmailCode = async (data: VerifyCodeRequest) => {
  const res = await authClient.post("/verification/verify", data);
  return res.data;
};