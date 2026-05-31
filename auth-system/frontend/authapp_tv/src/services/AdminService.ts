import apiClient from "@/config/apiClient";
import type {
  AdminAuditLog,
  AdminDashboardSummary,
  AdminFilterOptions,
  AdminIncident,
  AdminLoginHistory,
  AdminParams,
  AdminPasswordResetResponse,
  AdminRisk,
  AdminSession,
  AdminSettings,
  AdminUser,
  AdminUserDetail,
  PageResponse,
} from "@/models/AdminModels";

const cleanAdminParams = (params?: AdminParams) => {
  if (!params) return undefined;

  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== ""),
  );
};

// 관리자 홈 KPI를 조회
export const getAdminDashboardSummary = async () => {
  const response = await apiClient.get<AdminDashboardSummary>("/admin/dashboard/summary");
  return response.data;
};

export const getAdminFilterOptions = async () => {
  const response = await apiClient.get<AdminFilterOptions>("/admin/filter-options");
  return response.data;
};

// 사용자 목록/상세/계정 조치 API
export const getAdminUsers = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminUser>>("/admin/users", { params: cleanAdminParams(params) });
  return response.data;
};

export const getAdminUserDetail = async (username: string) => {
  const response = await apiClient.get<AdminUserDetail>(`/admin/users/${username}`);
  return response.data;
};

export const lockAdminUser = async (id: string | number) => {
  await apiClient.post(`/admin/users/${id}/lock`);
};

export const unlockAdminUser = async (id: string | number) => {
  await apiClient.post(`/admin/users/${id}/unlock`);
};

export const disableAdminUser = async (username: string) => {
  await apiClient.post(`/admin/users/${username}/disable`);
};

export const enableAdminUser = async (username: string) => {
  await apiClient.post(`/admin/users/${username}/enable`);
};

export const revokeAdminUserTokens = async (username: string) => {
  await apiClient.post(`/admin/users/${username}/tokens/revoke`);
};

export const resetAdminUserPassword = async (username: string) => {
  const response = await apiClient.post<AdminPasswordResetResponse>(`/admin/users/${username}/password/reset`);
  return response.data;
};

export const resetAdminUserMfa = async (username: string) => {
  await apiClient.post(`/admin/users/${username}/mfa/reset`);
};

// 감사/로그인/보안 이벤트 목록 API
export const getAdminAuditLogs = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminAuditLog>>("/admin/audit-logs", { params: cleanAdminParams(params) });
  return response.data;
};

export const getAdminLoginHistory = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminLoginHistory>>("/admin/login-history", { params: cleanAdminParams(params) });
  return response.data;
};

export const getAdminSecurityEvents = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminAuditLog>>("/admin/security-events", { params: cleanAdminParams(params) });
  return response.data;
};

// 보안 사고 해결 흐름 API
export const getAdminIncidents = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminIncident>>("/admin/incidents", { params: cleanAdminParams(params) });
  return response.data;
};

export const resolveAdminIncident = async (id: number) => {
  await apiClient.post(`/admin/incidents/${id}/resolve`);
};

// 세션/위험/설정 운영 API
export const getAdminSessions = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminSession>>("/admin/sessions", { params: cleanAdminParams(params) });
  return response.data;
};

export const revokeAdminSession = async (id: number) => {
  await apiClient.delete(`/admin/sessions/${id}`);
};

export const getAdminRisks = async (params?: AdminParams) => {
  const response = await apiClient.get<PageResponse<AdminRisk>>("/admin/risks", { params: cleanAdminParams(params) });
  return response.data;
};

export const getAdminSettings = async () => {
  const response = await apiClient.get<AdminSettings>("/admin/settings");
  return response.data;
};

export const updateAdminSettings = async (data: AdminSettings) => {
  const response = await apiClient.patch<AdminSettings>("/admin/settings", data);
  return response.data;
};
