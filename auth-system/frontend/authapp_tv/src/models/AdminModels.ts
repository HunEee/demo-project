export type AdminParams = Record<string, string | number | boolean | undefined>;

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type AdminUser = {
  id: number;
  username: string;
  name?: string;
  email?: string;
  nickname?: string;
  employeeNo?: string;
  department?: string;
  position?: string;
  employmentType?: string;
  status?: string;
  userType?: string;
  authMethod?: string;
  mfaEnabled: boolean;
  lastLoginAt?: string;
  locked: boolean;
  enabled: boolean;
  deleted: boolean;
  social: boolean;
  createdAt?: string;
  roles?: string[];
};

export type AdminDashboardSummary = {
  totalUsers: number;
  activeUsers: number;
  lockedUsers: number;
  adminUsers: number;
  totalSessions: number;
  revokedSessions: number;
  openIncidents: number;
  highRiskUsers: number;
};

export type AdminFilterOption = {
  label: string;
  value: string;
};

export type AdminFilterOptions = {
  userStatuses: AdminFilterOption[];
  roles: AdminFilterOption[];
  auditEventTypes: AdminFilterOption[];
  loginStatuses: AdminFilterOption[];
  incidentTypes: AdminFilterOption[];
  incidentSeverities: AdminFilterOption[];
  sessionStatuses: AdminFilterOption[];
  riskLevels: AdminFilterOption[];
};

export type AdminAuditLog = {
  id: number;
  username: string;
  type: string;
  description?: string;
  ipAddress?: string;
  device?: string;
  createdAt?: string;
};

export type AdminLoginHistory = {
  id: number;
  username: string;
  loginAt?: string;
  logoutAt?: string;
  success: boolean;
  status: string;
  failReason?: string;
  ipAddress?: string;
  device?: string;
  location?: string;
};

export type AdminIncident = {
  id: number;
  username: string;
  type: string;
  severity: string;
  description?: string;
  ipAddress?: string;
  device?: string;
  resolved: boolean;
  resolvedBy?: string;
  resolvedAt?: string;
  createdAt?: string;
};

export type AdminSession = {
  id: number;
  username: string;
  jti: string;
  device?: string;
  ipAddress?: string;
  revoked: boolean;
  createdAt?: string;
  expiresAt?: string;
  lastUsedAt?: string;
};

export type AdminRisk = {
  id: number;
  username: string;
  riskScore: number;
  riskLevel: string;
  lastReason?: string;
  updatedAt?: string;
};

export type AdminActionLog = {
  id: number;
  actorUsername: string;
  targetUsername: string;
  actionType: string;
  reason?: string;
  beforeValue?: string;
  afterValue?: string;
  ipAddress?: string;
  device?: string;
  createdAt?: string;
};

export type AdminPasswordResetResponse = {
  username: string;
  temporaryPassword: string;
};

export type AdminSettings = {
  maxLoginFailures: number;
  highRiskThreshold: number;
  criticalRiskThreshold: number;
  sessionExpireDays: number;
  forceLogoutOnCriticalRisk: boolean;
};

export type AdminUserDetail = {
  user: AdminUser;
  recentLogins: AdminLoginHistory[];
  recentEvents: AdminAuditLog[];
  sessions: AdminSession[];
  adminActions: AdminActionLog[];
  risk?: AdminRisk | null;
};
