import apiClient from "@/config/apiClient";
import type LoginHistoryData from "@/models/LoginHistoryData";

export const getLoginHistories = async (page = 0, size = 10, date?: string): Promise<LoginHistoryData> => {
  const res = await apiClient.get("/login-histories", {
    params: { page, size, date },
  });
  return res.data;
};