import apiClient from "@/config/apiClient";
import type {UserResponse, UpdateUserRequest, } from "@/models/user/UserModels";

// =============================
// 로그인 상태 유저 활동
// =============================

// 내 정보 조회
export const getMyInfo = async () => {
  const res = await apiClient.get("/users/me");
  return res.data as UserResponse;
};

// 유저 수정
export const updateUser = async (data: UpdateUserRequest) => {
  const res = await apiClient.patch("/users/me", data);
  return res.data;
};

// 회원 탈퇴
export const deleteUser = async () => {
  await apiClient.delete("/users/me");
};