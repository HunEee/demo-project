import authClient from "@/config/authClient";
import type {SignupRequest, CheckUsernameRequest, } from "@/models/user/UserModels";

// =======================================================================================
// 인증 없는 상태에서 호출하는 API(로그아웃 상태)
// =======================================================================================

// 회원가입
export const signup = async (data: SignupRequest) => {
  const res = await authClient.post("/users", data);
  return res.data; // userId
};

// username 중복 체크
export const checkUsername = async (data: CheckUsernameRequest) => {
  const res = await authClient.post("/users/exists", data);
  return res.data as boolean;
};