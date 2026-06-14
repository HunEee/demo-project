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
  unresolvedRiskEvents: number;
  criticalRiskEventsToday: number;
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

export type AdminRiskEvent = {
  id: number;
  username: string;
  eventType?: string;
  riskLevel?: string;
  score: number;
  description?: string;
  reason?: string;
  ipAddress?: string;
  userAgent?: string;
  device?: string;
  resolved: boolean;
  resolvedBy?: string;
  resolvedAt?: string;
  createdAt?: string;
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
  mfaPolicy: "OFF" | "OPTIONAL" | "REQUIRED_FOR_ADMIN" | "REQUIRED_FOR_ALL";
};

export type AdminSecurityPolicyCenter = {
  authentication: AdminAuthenticationPolicy;
  sessionToken: AdminTokenPolicy;
  riskDetection: AdminRiskSecurityPolicy;
  riskRules: AdminRiskRulePolicy[];
  operationalSecurity: AdminOperationalSecurityPolicy;
};

export type AdminAuthenticationPolicy = {
  login: AdminAuthPolicy;
  password: AdminPasswordPolicy;
  lockout: AdminLockoutPolicy;
  mfa: AdminMfaSecurityPolicy;
  verificationToken: AdminVerificationTokenPolicy;
};

export type AdminOperationalSecurityPolicy = {
  adminAccess: AdminAdminAccessPolicy;
  corsRedirect: AdminCorsRedirectPolicy;
  logRetention: AdminLogRetentionPolicy;
  rateLimit: AdminRateLimitPolicy;
};

export type AdminAuthPolicy = {
  localPasswordLoginEnabled: boolean;
  socialLoginEnabled: boolean;
  signupEnabled: boolean;
  rememberMeEnabled: boolean;
  defaultPostLoginRoute: string;
  genericLoginFailureMessageEnforced: boolean;
};

export type AdminPasswordPolicy = {
  minLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigit: boolean;
  requireSpecial: boolean;
  historyCount: number;
  expirationDays: number;
  temporaryPasswordExpirationMinutes: number;
  blockUsernameEmailInclusion: boolean;
};

export type AdminLockoutPolicy = {
  failedLoginThreshold: number;
  failureWindowMinutes: number;
  lockDurationMinutes: number;
  adminFailedLoginThreshold: number;
  autoUnlockEnabled: boolean;
  manualUnlockAuditRequired: boolean;
};

export type AdminMfaSecurityPolicy = {
  policy: "OFF" | "OPTIONAL" | "REQUIRED_FOR_ADMIN" | "REQUIRED_FOR_ALL";
  highRiskMfaThreshold: number;
  temporaryExceptionMaxDays: number;
  challengeFailureLimit: number;
  challengeExpirationMinutes: number;
};

export type AdminTokenPolicy = {
  accessTokenLifetimeMinutes: number;
  refreshTokenLifetimeDays: number;
  rotationEnabled: boolean;
  rotationGraceSeconds: number;
  allowRecentRotationRecovery: boolean;
  maxActiveSessionsUser: number;
  maxActiveSessionsAdmin: number;
  idleTimeoutMinutes: number;
  revokeOnPasswordChange: boolean;
  revokeOnMfaReset: boolean;
};

export type AdminRiskSecurityPolicy = {
  mediumThreshold: number;
  highThreshold: number;
  criticalThreshold: number;
  mfaRequiredScore: number;
  tokenRevokeScore: number;
  loginBlockScore: number;
  revokeAllSessionsScore: number;
  autoResponseEnabled: boolean;
  firstLoginNewIpExempt: boolean;
  firstLoginNewUserAgentExempt: boolean;
  tokenReuseForceCritical: boolean;
  tokenReuseRevokeAllSessions: boolean;
  tokenContextChangeRevokeFamily: boolean;
};

export type AdminRiskRulePolicy = {
  id?: number;
  eventType: string;
  enabled: boolean;
  score: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  thresholdCount?: number | null;
  windowMinutes?: number | null;
  nightStartHour?: number | null;
  nightEndHour?: number | null;
  description?: string | null;
};

export type AdminAdminAccessPolicy = {
  adminIpAllowlistEnabled: boolean;
  denyUnknownProxyHeaders: boolean;
  trustedProxyHeaderMode: string;
  requireMfaForAdminAccess: boolean;
  adminSessionMaxAgeMinutes: number;
  allowedAdminIps?: string | null;
};

export type AdminCorsRedirectPolicy = {
  allowCredentials: boolean;
  allowedMethods: string;
  allowedOrigins?: string | null;
  allowedRedirectUris?: string | null;
};

export type AdminVerificationTokenPolicy = {
  signupTokenExpirationMinutes: number;
  passwordResetTokenExpirationMinutes: number;
  maxVerificationAttempts: number;
  resendCooldownSeconds: number;
  dailySendLimitPerEmail: number;
  invalidatePreviousTokenOnResend: boolean;
};

export type AdminLogRetentionPolicy = {
  auditLogRetentionDays: number;
  loginHistoryRetentionDays: number;
  riskEventRetentionDays: number;
  securityIncidentRetentionDays: number;
  adminActionLogRetentionDays: number;
  exportRequiresReason: boolean;
  auditLogDeleteDisabled: boolean;
  archiveBeforePurge: boolean;
};

export type AdminRateLimitPolicy = {
  loginPerIpPerMinute: number;
  loginPerUsernamePerMinute: number;
  refreshPerSessionPerMinute: number;
  passwordResetPerEmailPerDay: number;
  verificationEmailPerEmailPerDay: number;
  adminWritePerMinute: number;
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
