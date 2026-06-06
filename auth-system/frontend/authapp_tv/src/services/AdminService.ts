import apiClient from "@/config/apiClient";
import type {
  AdminApiPermissionRule,
  AdminApiPermissionRuleRequest,
  AdminAuditLog,
  AdminDashboardSummary,
  AdminDuplicateCheckResponse,
  AdminFilterOptions,
  AdminGroup,
  AdminGroupDetail,
  AdminGroupMemberRequest,
  AdminGroupRequest,
  AdminGroupRoleRequest,
  AdminIncident,
  AdminLoginHistory,
  AdminParams,
  AdminPasswordResetResponse,
  AdminPermission,
  AdminPermissionRequest,
  AdminRisk,
  AdminRole,
  AdminRoleAssignmentHistory,
  AdminRoleAssignmentRequest,
  AdminRoleDetail,
  AdminRolePermissionRequest,
  AdminRoleRequest,
  AdminSession,
  AdminSettings,
  AdminUser,
  AdminUserCreateRequest,
  AdminUserDetail,
  AdminUserUpdateRequest,
  HrUserMaster,
  HrUserMasterRequest,
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

export const createAdminUser = async (data: AdminUserCreateRequest) => {
  const response = await apiClient.post<AdminUser>("/admin/users", data);
  return response.data;
};

export const checkAdminUsernameExists = async (username: string) => {
  const response = await apiClient.get<AdminDuplicateCheckResponse>("/admin/users/duplicate-check", {
    params: cleanAdminParams({ username }),
  });
  return response.data;
};

export const updateAdminUser = async (username: string, data: AdminUserUpdateRequest) => {
  const response = await apiClient.patch<AdminUser>(`/admin/users/${username}`, data);
  return response.data;
};

export const deleteAdminUser = async (username: string, reason?: string) => {
  await apiClient.post(`/admin/users/${username}/delete`, undefined, { params: cleanAdminParams({ reason }) });
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

export const assignAdminUserRole = async (username: string, data: AdminRoleAssignmentRequest) => {
  await apiClient.post(`/admin/users/${username}/roles`, data);
};

export const removeAdminUserRole = async (username: string, roleId: number, reason?: string) => {
  await apiClient.delete(`/admin/users/${username}/roles/${roleId}`, {
    params: cleanAdminParams({ reason }),
  });
};

// 조직/부서 관리 API
export const getHrUserMasters = async (params?: AdminParams) => {
  const response = await apiClient.get<HrUserMaster[]>("/admin/hr-users", { params: cleanAdminParams(params) });
  return response.data;
};

export const getHrUserAccountCandidates = async () => {
  const response = await apiClient.get<HrUserMaster[]>("/admin/hr-users/candidates");
  return response.data;
};

export const createHrUserMaster = async (data: HrUserMasterRequest) => {
  const response = await apiClient.post<HrUserMaster>("/admin/hr-users", data);
  return response.data;
};

export const updateHrUserMaster = async (id: number, data: HrUserMasterRequest) => {
  const response = await apiClient.patch<HrUserMaster>(`/admin/hr-users/${id}`, data);
  return response.data;
};

export const deleteHrUserMaster = async (id: number) => {
  await apiClient.delete(`/admin/hr-users/${id}`);
};

export const checkHrUserMasterExists = async (field: string, value: string) => {
  const response = await apiClient.get<AdminDuplicateCheckResponse>("/admin/hr-users/duplicate-check", {
    params: cleanAdminParams({ field, value }),
  });
  return response.data;
};

// 그룹 관리 API
export const getAdminGroups = async () => {
  const response = await apiClient.get<AdminGroup[]>("/admin/groups");
  return response.data;
};

export const getAdminGroupDetail = async (id: number) => {
  const response = await apiClient.get<AdminGroupDetail>(`/admin/groups/${id}`);
  return response.data;
};

export const createAdminGroup = async (data: AdminGroupRequest) => {
  const response = await apiClient.post<AdminGroup>("/admin/groups", data);
  return response.data;
};

export const updateAdminGroup = async (id: number, data: AdminGroupRequest) => {
  const response = await apiClient.patch<AdminGroup>(`/admin/groups/${id}`, data);
  return response.data;
};

export const disableAdminGroup = async (id: number, reason?: string) => {
  await apiClient.post(`/admin/groups/${id}/disable`, undefined, { params: cleanAdminParams({ reason }) });
};

export const addAdminGroupMember = async (id: number, data: AdminGroupMemberRequest) => {
  const response = await apiClient.post<AdminGroupDetail>(`/admin/groups/${id}/members`, data);
  return response.data;
};

export const removeAdminGroupMember = async (id: number, username: string, reason?: string) => {
  const response = await apiClient.delete<AdminGroupDetail>(`/admin/groups/${id}/members/${username}`, {
    params: cleanAdminParams({ reason }),
  });
  return response.data;
};

export const assignAdminGroupRole = async (id: number, data: AdminGroupRoleRequest) => {
  const response = await apiClient.post<AdminGroupDetail>(`/admin/groups/${id}/roles`, data);
  return response.data;
};

export const removeAdminGroupRole = async (id: number, roleId: number, reason?: string) => {
  const response = await apiClient.delete<AdminGroupDetail>(`/admin/groups/${id}/roles/${roleId}`, {
    params: cleanAdminParams({ reason }),
  });
  return response.data;
};

// 감사/로그인/보안 이벤트 목록 API
export const getAdminRoles = async () => {
  const response = await apiClient.get<AdminRole[]>("/admin/roles");
  return response.data;
};

export const getAdminRoleDetail = async (id: number) => {
  const response = await apiClient.get<AdminRoleDetail>(`/admin/roles/${id}`);
  return response.data;
};

export const createAdminRole = async (data: AdminRoleRequest) => {
  const response = await apiClient.post<AdminRole>("/admin/roles", data);
  return response.data;
};

export const updateAdminRole = async (id: number, data: AdminRoleRequest) => {
  const response = await apiClient.patch<AdminRole>(`/admin/roles/${id}`, data);
  return response.data;
};

export const disableAdminRole = async (id: number) => {
  await apiClient.post(`/admin/roles/${id}/disable`);
};

export const deleteAdminRole = async (id: number) => {
  await apiClient.delete(`/admin/roles/${id}`);
};

export const assignAdminRolePermission = async (id: number, data: AdminRolePermissionRequest) => {
  const response = await apiClient.post<AdminRoleDetail>(`/admin/roles/${id}/permissions`, data);
  return response.data;
};

export const removeAdminRolePermission = async (id: number, permissionId: number, reason?: string) => {
  const response = await apiClient.delete<AdminRoleDetail>(`/admin/roles/${id}/permissions/${permissionId}`, {
    params: cleanAdminParams({ reason }),
  });
  return response.data;
};

export const getAdminPermissions = async () => {
  const response = await apiClient.get<AdminPermission[]>("/admin/permissions");
  return response.data;
};

export const createAdminPermission = async (data: AdminPermissionRequest) => {
  const response = await apiClient.post<AdminPermission>("/admin/permissions", data);
  return response.data;
};

export const updateAdminPermission = async (id: number, data: AdminPermissionRequest) => {
  const response = await apiClient.patch<AdminPermission>(`/admin/permissions/${id}`, data);
  return response.data;
};

export const deleteAdminPermission = async (id: number) => {
  await apiClient.delete(`/admin/permissions/${id}`);
};

export const getAdminRoleAssignmentHistory = async () => {
  const response = await apiClient.get<AdminRoleAssignmentHistory[]>("/admin/role-assignment-history");
  return response.data;
};

export const getAdminApiPermissionRules = async () => {
  const response = await apiClient.get<AdminApiPermissionRule[]>("/admin/api-permission-rules");
  return response.data;
};

export const createAdminApiPermissionRule = async (data: AdminApiPermissionRuleRequest) => {
  const response = await apiClient.post<AdminApiPermissionRule>("/admin/api-permission-rules", data);
  return response.data;
};

export const updateAdminApiPermissionRule = async (id: number, data: AdminApiPermissionRuleRequest) => {
  const response = await apiClient.patch<AdminApiPermissionRule>(`/admin/api-permission-rules/${id}`, data);
  return response.data;
};

export const deleteAdminApiPermissionRule = async (id: number) => {
  await apiClient.delete(`/admin/api-permission-rules/${id}`);
};

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
