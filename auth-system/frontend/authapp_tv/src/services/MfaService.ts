import apiClient from "@/config/apiClient";
import authClient from "@/config/authClient";
import type {
  AdminMfaUserResponse,
  MfaMethodResponse,
  MfaMethodType,
  MfaPolicy,
  MfaPolicyResponse,
  PreAuthTotpConfirmRequest,
  PreAuthTotpSetupResponse,
  TotpSetupResponse,
} from "@/models/MfaModels";
import type LoginResponseData from "@/models/LoginResponseData";

export const setupTotp = async () => {
  const response = await apiClient.post<TotpSetupResponse>("/mfa/totp/setup");
  return response.data;
};

export const confirmTotp = async (methodId: number, code: string) => {
  const response = await apiClient.post<MfaMethodResponse>("/mfa/totp/confirm", { methodId, code });
  return response.data;
};

export const getMfaMethods = async () => {
  const response = await apiClient.get<MfaMethodResponse[]>("/mfa/methods");
  return response.data;
};

export const deleteMfaMethod = async (id: number, code: string) => {
  await apiClient.delete(`/mfa/methods/${id}`, { data: { code } });
};

export const verifyMfaLogin = async (challengeId: string, methodType: MfaMethodType, code: string) => {
  const response = await authClient.post<LoginResponseData>("/auth/mfa/verify", { challengeId, methodType, code });
  return response.data;
};

export const setupPreAuthTotp = async (challengeId: string) => {
  const response = await authClient.post<PreAuthTotpSetupResponse>("/auth/mfa/totp/setup", { challengeId });
  return response.data;
};

export const confirmPreAuthTotp = async (request: PreAuthTotpConfirmRequest) => {
  const response = await authClient.post<LoginResponseData>("/auth/mfa/totp/confirm", request);
  return response.data;
};

export const getAdminMfaUsers = async () => {
  const response = await apiClient.get<AdminMfaUserResponse[]>("/admin/mfa/users");
  return response.data;
};

export const resetAdminUserMfa = async (username: string, reason: string) => {
  await apiClient.post(`/admin/mfa/users/${username}/reset`, { reason });
};

export const createAdminMfaException = async (username: string, reason: string, expiresAt: string) => {
  await apiClient.post(`/admin/mfa/users/${username}/exception`, { reason, expiresAt });
};

export const revokeAdminMfaException = async (username: string) => {
  await apiClient.delete(`/admin/mfa/users/${username}/exception`);
};

export const getAdminMfaPolicy = async () => {
  const response = await apiClient.get<MfaPolicyResponse>("/admin/mfa/policy");
  return response.data;
};

export const updateAdminMfaPolicy = async (policy: MfaPolicy) => {
  const response = await apiClient.patch<MfaPolicyResponse>("/admin/mfa/policy", { policy });
  return response.data;
};
