import apiClient from "@/config/apiClient";
import type SessionData from "@/models/SessionData";

// 세션 목록 조회
export const getSessions = async (): Promise<SessionData[]> => {
  const res = await apiClient.get("/me/sessions");
  return res.data;
};

// 개별 로그아웃
export const logoutSession = async (id: string) => {
  await apiClient.delete(`/me/sessions/${id}`);
};

// 전체 로그아웃
export const logoutAllSessions = async () => {
  await apiClient.delete("/sessions");
};
