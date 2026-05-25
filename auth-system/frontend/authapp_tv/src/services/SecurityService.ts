import apiClient from "@/config/apiClient";
import type SecurityStatus from "@/models/SecurityStatus";

export const getSecurityStatus = async () => {
  const response = await apiClient.get<SecurityStatus>("/security");
  return response.data;
};
