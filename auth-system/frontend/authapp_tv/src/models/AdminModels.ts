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
  departmentCode?: string;
  department?: string;
  position?: string;
  employmentType?: string;
  joinedAt?: string;
  leftAt?: string;
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
  groups?: string[];
};

export type AdminUserCreateRequest = {
  employeeNo: string;
  username: string;
  password: string;
  roleName?: string;
  reason?: string;
};

export type AdminDuplicateCheckResponse = {
  field: string;
  value: string;
  exists: boolean;
};

export type AdminUserUpdateRequest = {
  email?: string;
  name?: string;
  locked?: boolean;
  enabled?: boolean;
  reason?: string;
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
  revokedReason?: string;
  revokedAt?: string;
  revokedBy?: string;
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
  targetType?: string;
  targetId?: string;
  targetUsername?: string;
  targetName?: string;
  actionType: string;
  reason?: string;
  beforeValue?: string;
  afterValue?: string;
  ipAddress?: string;
  device?: string;
  userAgent?: string;
  result?: string;
  riskLevel?: string;
  metadata?: string;
  createdAt?: string;
};

export type RiskActionLog = {
  id: number;
  username: string;
  riskId?: number;
  riskEventId?: number;
  riskLevel: string;
  action: string;
  mode: "AUTO" | "MANUAL";
  status: "SUCCESS" | "FAILED" | "SKIPPED";
  reason?: string;
  actorUsername?: string;
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

export type HrUserMaster = {
  id: number;
  employeeNo: string;
  name: string;
  email: string;
  phone?: string | null;
  departmentCode?: string | null;
  departmentName?: string | null;
  position?: string | null;
  employmentType: string;
  hrStatus: string;
  accountStatus: string;
  accountUsername?: string | null;
  joinedAt?: string | null;
  leftAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type HrUserMasterRequest = {
  employeeNo: string;
  name: string;
  email?: string;
  phone?: string;
  departmentCode?: string;
  departmentName?: string;
  position?: string;
  employmentType?: string;
  hrStatus?: string;
  joinedAt?: string;
  leftAt?: string;
};

export type AdminGroup = {
  id: number;
  name: string;
  type: string;
  ownerUsername?: string | null;
  description?: string | null;
  enabled: boolean;
  userCount: number;
  roleCount: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AdminGroupRequest = {
  name: string;
  type: string;
  ownerUsername?: string | null;
  description?: string | null;
  enabled?: boolean;
  reason?: string;
};

export type AdminGroupMemberRequest = {
  username: string;
  reason?: string;
};

export type AdminGroupRoleRequest = {
  roleName: string;
  reason?: string;
  sensitiveReason?: string;
};

export type AdminGroupMember = {
  username: string;
  name?: string;
  email?: string;
  createdAt?: string;
};

export type AdminGroupRole = {
  roleId: number;
  roleName: string;
  createdAt?: string;
};

export type AdminGroupDetail = {
  group: AdminGroup;
  members: AdminGroupMember[];
  roles: AdminGroupRole[];
};

export type AdminUserDetail = {
  user: AdminUser;
  recentLogins: AdminLoginHistory[];
  recentEvents: AdminAuditLog[];
  sessions: AdminSession[];
  adminActions: AdminActionLog[];
  risk?: AdminRisk | null;
  groups: AdminGroup[];
};

export type AdminPermission = {
  id: number;
  code: string;
  name: string;
  category?: string | null;
  description?: string | null;
  sensitive: boolean;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type AdminPermissionRequest = {
  code: string;
  name: string;
  category?: string;
  description?: string;
  sensitive?: boolean;
  enabled?: boolean;
  reason?: string;
};

export type AdminRole = {
  id: number;
  name: string;
  displayName?: string | null;
  description?: string | null;
  enabled: boolean;
  systemRole: boolean;
  sensitive: boolean;
  permissionCount: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AdminRoleRequest = {
  name: string;
  displayName?: string;
  description?: string;
  enabled?: boolean;
  systemRole?: boolean;
  reason?: string;
};

export type AdminRoleDetail = {
  role: AdminRole;
  permissions: AdminPermission[];
};

export type AdminRolePermissionRequest = {
  permissionId?: number;
  permissionCode?: string;
  reason?: string;
};

export type AdminRoleAssignmentRequest = {
  roleId?: number;
  roleName?: string;
  reason?: string;
  sensitiveReason?: string;
};

export type AdminRoleAssignmentHistory = {
  id: number;
  targetType: string;
  targetId: string;
  targetName?: string | null;
  roleId: number;
  roleName: string;
  action: string;
  actorUsername?: string | null;
  reason?: string | null;
  sensitive: boolean;
  sensitiveReason?: string | null;
  createdAt?: string;
};

export type AdminApiPermissionRule = {
  id: number;
  httpMethod: string;
  pathPattern: string;
  permissionCode: string;
  description?: string | null;
  enabled: boolean;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AdminApiPermissionRuleRequest = {
  httpMethod: string;
  pathPattern: string;
  permissionCode: string;
  description?: string;
  enabled?: boolean;
  sortOrder?: number;
  reason?: string;
};
