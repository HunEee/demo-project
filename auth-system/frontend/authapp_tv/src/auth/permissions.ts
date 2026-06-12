import type User from "@/models/User";

export const ADMIN_READ_PERMISSIONS = [
  "ADMIN_ADMIN_READ",
  "ADMIN_DASHBOARD_READ",
  "ADMIN_USERS_READ",
  "ADMIN_ROLES_READ",
  "ADMIN_PERMISSIONS_READ",
  "ADMIN_GROUPS_READ",
  "ADMIN_HR_USERS_READ",
  "ADMIN_AUDIT_READ",
  "ADMIN_SETTINGS_READ",
] as const;

export const hasPermission = (user: User | null | undefined, permission: string) =>
  Boolean(user?.permissions?.includes(permission));

export const hasAnyPermission = (user: User | null | undefined, permissions: readonly string[]) =>
  permissions.some((permission) => hasPermission(user, permission));

export const canAccessMenuItem = (
  user: User | null | undefined,
  item: { requiredPermissions?: readonly string[] },
) => {
  if (!item.requiredPermissions || item.requiredPermissions.length === 0) {
    return hasAdminAccess(user);
  }

  if (user?.permissions && user.permissions.length > 0) {
    return hasAnyPermission(user, item.requiredPermissions);
  }

  return user?.roles?.includes("ROLE_ADMIN") ?? false;
};

export const hasAdminAccess = (user: User | null | undefined) => {
  if (!user) return false;
  if (user.permissions && user.permissions.length > 0) {
    return hasAnyPermission(user, ADMIN_READ_PERMISSIONS);
  }

  return user.roles?.includes("ROLE_ADMIN") ?? false;
};
