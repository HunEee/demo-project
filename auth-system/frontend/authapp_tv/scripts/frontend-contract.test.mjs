// 파일 읽기
import { readFileSync } from "node:fs";
// 경로 조합
import { resolve } from "node:path";
// 테스트용 assert
import assert from "node:assert/strict";

// 프로젝트 루트 경로: 현재 파일 위치 기준으로 상위 폴더(..)를 root로 설정
const root = resolve(import.meta.dirname, "..");

/**
 * 파일 읽기 헬퍼 함수
 * - path: 프로젝트 루트 기준 상대 경로
 * - 반환: 파일 문자열 내용
 */
const read = (path) => readFileSync(resolve(root, path), "utf8");

/* =========================================================
 * AuthService 테스트
 * ========================================================= */

// AuthService.ts 파일 읽기
const authService = read("src/services/AuthService.ts");

/**
 * - getCurrentUser()가 반드시 GET /users/me를 호출해야 하는지 검사
 * - 정규식: apiClient.get<User>("/users/me")
 */
assert.match(
  authService,
  /apiClient\.get<User>\(["'`]\/users\/me["'`]\)/,
  "getCurrentUser() must call GET /users/me",
);

/**
 * 기존 레거시 API:
 * GET /user -> 더 이상 사용하면 안 되는지 검사
 */
assert.doesNotMatch(
  authService,
  /apiClient\.get<User>\(["'`]\/user["'`]\)/,
  "getCurrentUser() must not call legacy GET /user",
);

/* =========================================================
 * React Router 구조 테스트
 * ========================================================= */

// main.tsx 파일 읽기(공백/줄바꿈 제거해서 문자열 검색을 쉽게 만듦)
const main = read("src/main.tsx").replace(/\s+/g, " ");

/**
 * /mypage Route 위치 찾기
 * 기대 구조: <Route path="/mypage" element={<UserLayout />}>
 */
const mypageRouteIndex = main.indexOf('<Route path="/mypage" element={<UserLayout />}>');

/**
 * /mypage Route가 반드시 존재해야 함
 */
assert.notEqual(
  mypageRouteIndex,
  -1,
  "/mypage must render through UserLayout",
);

/**
 * /mypage 하위에 반드시 포함되어야 하는 child routes 목록
 */
for (const childRoute of [
  '<Route index element={<MyPage />} />',
  '<Route path="login-history" element={<LoginHistoryPage />} />',
  '<Route path="security" element={<SecurityPage />} />',
  '<Route path="password" element={<ChangePasswordPage />} />',
  '<Route path="sessions" element={<SessionsPage />} />',
]) {

  // child route 위치 찾기
  const childIndex = main.indexOf(childRoute, mypageRouteIndex);

  /**
   * /mypage Route 종료 태그 위치
   */
  const closeIndex = main.indexOf("</Route>", mypageRouteIndex);

  /**
   * child route가 /mypage Route 내부에 존재하는지 검사
   */
  assert.ok(
    childIndex > mypageRouteIndex &&
      childIndex < closeIndex,

    `${childRoute} must be nested under the /mypage UserLayout route`,
  );
}

const adminRouteIndex = main.indexOf('<Route path="/admin" element={<UserLayout />}>');
assert.notEqual(
  adminRouteIndex,
  -1,
  "/admin must render through UserLayout",
);

for (const childRoute of [
  '<Route index element={<DashboardPage />} />',
  '<Route path="status/dashboard" element={<DashboardPage />} />',
  '<Route path="account/users" element={<UserManagementPage />} />',
  '<Route path="account/users/:username" element={<UserDetailPage />} />',
  '<Route path="account/organization" element={<OrganizationPage />} />',
  '<Route path="account/groups" element={<GroupManagementPage />} />',
  '<Route path="account/external-users" element={<ExternalUsersPage />} />',
  '<Route path="audit/logs" element={<AuditLogsPage />} />',
  '<Route path="audit/login-history" element={<AdminLoginHistoryPage />} />',
  '<Route path="security/events" element={<SecurityEventsPage />} />',
  '<Route path="security/sessions" element={<LoginSessionManagementPage />} />',
  '<Route path="security/risk-logins" element={<RiskLoginDetectionPage />} />',
  '<Route path="system/settings" element={<SystemSettingsPage />} />',
]) {
  const childIndex = main.indexOf(childRoute, adminRouteIndex);
  const closeIndex = main.indexOf("</Route>", adminRouteIndex);
  assert.ok(
    childIndex > adminRouteIndex && childIndex < closeIndex,
    `${childRoute} must be nested under the /admin UserLayout route`,
  );
}

/* =========================================================
 * AdminService 테스트
 * ========================================================= */

const adminService = read("src/services/AdminService.ts");
const adminModels = read("src/models/AdminModels.ts");
assert.match(
  adminService,
  /from ["'`]@\/models\/AdminModels["'`]/,
  "AdminService must import admin API types from AdminModels",
);
assert.doesNotMatch(
  adminService,
  /export type Admin(User|DashboardSummary|FilterOptions|AuditLog|LoginHistory|Incident|Session|Risk|Settings|UserDetail|Params)\b/,
  "AdminService must keep admin API types in src/models/AdminModels.ts",
);
for (const modelType of [
  "AdminParams",
  "PageResponse",
  "AdminUser",
  "AdminDashboardSummary",
  "AdminFilterOptions",
  "AdminAuditLog",
  "AdminLoginHistory",
  "AdminIncident",
  "AdminSession",
  "AdminRisk",
  "AdminSettings",
  "AdminUserDetail",
]) {
  assert.match(adminModels, new RegExp(`export type ${modelType}`), `AdminModels must export ${modelType}`);
}
assert.match(
  adminService,
  /const cleanAdminParams = /,
  "AdminService must remove empty filter values before sending admin requests",
);
assert.match(
  adminService,
  /apiClient\.get<PageResponse<AdminUser>>\(["'`]\/admin\/users["'`], \{ params: cleanAdminParams\(params\) \}/,
  "AdminService must load users from GET /admin/users",
);
assert.match(
  adminService,
  /apiClient\.post\(`\/admin\/users\/\$\{id\}\/lock`\)/,
  "AdminService must lock users through POST /admin/users/{id}/lock",
);
assert.match(
  adminService,
  /apiClient\.post\(`\/admin\/users\/\$\{id\}\/unlock`\)/,
  "AdminService must unlock users through POST /admin/users/{id}/unlock",
);

for (const method of [
  "getAdminDashboardSummary",
  "getAdminFilterOptions",
  "getAdminUserDetail",
  "getAdminAuditLogs",
  "getAdminLoginHistory",
  "getAdminSecurityEvents",
  "getAdminIncidents",
  "resolveAdminIncident",
  "getAdminSessions",
  "revokeAdminSession",
  "getAdminRisks",
  "getAdminSettings",
  "updateAdminSettings",
]) {
  assert.match(
    adminService,
    new RegExp(`export const ${method} = async`),
    `AdminService must export ${method}`,
  );
}

assert.match(
  adminService,
  /apiClient\.get<AdminFilterOptions>\(["'`]\/admin\/filter-options["'`]\)/,
  "AdminService must load stable filter options from GET /admin/filter-options",
);

for (const page of [
  "AdminUsersPage",
  "AdminUserDetailPage",
  "AdminAuditLogsPage",
  "AdminLoginHistoryPage",
  "AdminSecurityEventsPage",
  "AdminIncidentsPage",
  "AdminSessionsPage",
  "AdminRiskPage",
  "AdminSettingsPage",
]) {
  const adminPage = read(`src/pages/admin/${page}.tsx`);
  assert.match(
    adminPage,
    /AdminPageShell/,
    `${page} must use the shared admin shell so header and body alignment stay consistent`,
  );
  assert.doesNotMatch(
    adminPage,
    /type Admin[A-Za-z]+ \} from ["'`]@\/services\/AdminService["'`]/,
    `${page} must import admin types from AdminModels instead of AdminService`,
  );
}

const adminShell = read("src/pages/admin/AdminPageShell.tsx");
assert.match(
  adminShell,
  /max-w-6xl/,
  "AdminPageShell must center header and body in the same max-width container",
);

const adminUi = read("src/pages/admin/adminUi.tsx");
assert.match(
  adminUi,
  /adminCellClassName = ".*text-center/,
  "Admin table cells must center header and body content consistently",
);
assert.match(
  adminUi,
  /AdminSortableHeader/,
  "Admin tables must expose a reusable sortable header",
);
assert.match(
  adminUi,
  /AdminPagination/,
  "Admin tables must expose reusable pagination controls",
);

const adminPage = read("src/pages/AdminPage.tsx");
assert.match(
  adminPage,
  /unit: "紐?/,
  "Admin dashboard user metrics must show people units",
);
assert.match(
  adminPage,
  /unit: "嫄?/,
  "Admin dashboard incident/risk metrics must show count units",
);

const adminFilters = read("src/pages/admin/AdminFilters.tsx");
assert.match(
  adminFilters,
  /<div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">/,
  "Admin filter inputs must stay in their own four-column grid",
);
assert.match(
  adminFilters,
  /justify-between/,
  "Admin filter footer must keep server filter text on the left and action buttons on the right",
);

const adminSettingsPage = read("src/pages/admin/AdminSettingsPage.tsx");
assert.match(
  adminSettingsPage,
  /className="[^"]*mx-auto[^"]*max-w-3xl/,
  "AdminSettingsPage settings card must be centered in the admin content area",
);

for (const page of ["AdminUsersPage", "AdminIncidentsPage", "AdminSessionsPage"]) {
  assert.doesNotMatch(
    read(`src/pages/admin/${page}.tsx`),
    /adminCellClassName\} text-right/,
    `${page} action column must not override centered table alignment`,
  );
}

for (const [page, params] of [
  ["AdminUsersPage", ["keyword", "status", "role"]],
  ["AdminAuditLogsPage", ["username", "type", "from", "to"]],
  ["AdminLoginHistoryPage", ["username", "status", "from", "to"]],
  ["AdminSecurityEventsPage", ["username", "type", "from", "to"]],
  ["AdminIncidentsPage", ["username", "type", "severity", "resolved", "from", "to"]],
  ["AdminSessionsPage", ["username", "status", "from", "to"]],
  ["AdminRiskPage", ["username", "level", "minScore"]],
]) {
  const adminPage = read(`src/pages/admin/${page}.tsx`);
  assert.match(adminPage, /AdminFilters/, `${page} must render admin filters`);
  assert.match(adminPage, /AdminSortableHeader/, `${page} must render sortable table headers`);
  assert.match(adminPage, /AdminPagination/, `${page} must render pagination controls`);
  assert.match(adminPage, /page: (nextPage|pageState\.page)/, `${page} must send page state to the API`);
  assert.match(adminPage, /sort: (nextSort|sortState)\.sort/, `${page} must send sort state to the API`);
  assert.match(adminPage, /direction: (nextSort|sortState)\.direction/, `${page} must send sort direction to the API`);
  assert.match(adminPage, /filterOptions/, `${page} must use admin filter options from the server`);
  assert.doesNotMatch(adminPage, /uniqueFilterOptions/, `${page} must not derive select filter options from only current page rows`);
  for (const param of params) {
    assert.match(adminPage, new RegExp(`${param}: filters\\.${param}`), `${page} must send ${param} filter to the API`);
  }
}

assert.doesNotMatch(
  read("src/pages/admin/AdminSessionsPage.tsx"),
  /device: filters\.device|name: "device"/,
  "AdminSessionsPage must not expose a device filter",
);

const adminUsersPage = read("src/pages/admin/AdminUsersPage.tsx");
assert.match(adminUsersPage, /item\.deleted/, "AdminUsersPage must render deleted account status separately");
assert.match(adminUsersPage, /employmentType: filters\.employmentType/, "AdminUsersPage must send employment type to the server");
assert.match(adminUsersPage, /mfaEnabled: filters\.mfaEnabled/, "AdminUsersPage must send MFA filter to the server");

for (const [page, forbidden] of [
  ["account/OrganizationPage.tsx", /AdminPlaceholderPage/],
  ["account/GroupManagementPage.tsx", /AdminPlaceholderPage/],
  ["account/ExternalUsersPage.tsx", /AdminPlaceholderPage/],
]) {
  assert.doesNotMatch(
    read(`src/pages/admin/${page}`),
    forbidden,
    `${page} must be a real account management screen, not a placeholder`,
  );
}

for (const modelType of [
  "AdminUserCreateRequest",
  "AdminUserUpdateRequest",
  "AdminDepartment",
  "AdminDepartmentRequest",
  "AdminDepartmentUserRequest",
  "AdminDepartmentUser",
  "AdminGroup",
  "AdminGroupDetail",
  "AdminGroupRequest",
  "AdminGroupMemberRequest",
]) {
  assert.match(adminModels, new RegExp(`export type ${modelType}`), `AdminModels must export ${modelType}`);
}

for (const serviceMethod of [
  "createAdminUser",
  "updateAdminUser",
  "deleteAdminUser",
  "getAdminDepartments",
  "createAdminDepartment",
  "updateAdminDepartment",
  "disableAdminDepartment",
  "getAdminDepartmentUsers",
  "addAdminDepartmentUser",
  "updateAdminDepartmentUser",
  "removeAdminDepartmentUser",
  "getAdminGroups",
  "getAdminGroupDetail",
  "createAdminGroup",
  "updateAdminGroup",
  "addAdminGroupMember",
  "removeAdminGroupMember",
  "assignAdminGroupRole",
  "removeAdminGroupRole",
]) {
  assert.match(adminService, new RegExp(`export const ${serviceMethod} = async`), `AdminService must export ${serviceMethod}`);
}

assert.match(
  read("src/pages/admin/account/ExternalUsersPage.tsx"),
  /employmentType: "EXTERNAL"/,
  "ExternalUsersPage must use the shared user API with employmentType=EXTERNAL",
);

for (const page of [
  "AdminUsersPage.tsx",
  "account/OrganizationPage.tsx",
  "account/GroupManagementPage.tsx",
  "account/ExternalUsersPage.tsx",
]) {
  const source = read(`src/pages/admin/${page}`);
  assert.match(source, /AdminCrudModal/, `${page} must use a centered modal for add/edit workflows`);
  assert.doesNotMatch(source, /AdminCrudDrawer/, `${page} must not use the side drawer for add/edit workflows`);
  assert.match(source, /AdminConfirmDialog/, `${page} must confirm destructive delete/disable workflows`);
}

for (const page of ["account/OrganizationPage.tsx", "account/GroupManagementPage.tsx"]) {
  const source = read(`src/pages/admin/${page}`);
  assert.match(source, /AdminFilters/, `${page} must start with the same filter pattern as the user management list`);
  assert.match(source, /AdminPagination/, `${page} must paginate the initial list view like user management`);
  assert.match(source, /AdminSortableHeader/, `${page} must support sortable list columns like user management`);
}

const organizationPage = read("src/pages/admin/account/OrganizationPage.tsx");
assert.match(organizationPage, /detailDepartment/, "OrganizationPage must open department detail in a modal state");
assert.match(organizationPage, /departmentUserFilters/, "Department detail modal must provide user search filters");
assert.match(organizationPage, /getAdminUsers/, "OrganizationPage must validate department additions against existing users");
assert.match(organizationPage, /addAdminDepartmentUser/, "OrganizationPage must add existing users to a department");
assert.match(organizationPage, /updateAdminDepartmentUser/, "OrganizationPage must edit department user profile data");
assert.match(organizationPage, /removeAdminDepartmentUser/, "OrganizationPage must remove users from a department");

const adminUiCrud = read("src/pages/admin/adminUi.tsx");
assert.match(adminUiCrud, /export function AdminCrudModal/, "adminUi must expose a reusable centered CRUD modal");
assert.doesNotMatch(adminUiCrud, /right-0 top-0 h-dvh/, "adminUi CRUD modal must not be styled as a side drawer");
assert.match(adminUiCrud, /export function AdminConfirmDialog/, "adminUi must expose a reusable confirm dialog");

const adminUserResponse = read("../../backend/authapp/src/main/java/com/example/authapp/domain/admin/dto/AdminUserResponse.java");
assert.match(adminUserResponse, /boolean deleted/, "AdminUserResponse must expose deleted account state");
const adminConsoleService = read("../../backend/authapp/src/main/java/com/example/authapp/domain/admin/AdminConsoleService.java");
assert.match(adminConsoleService, /case "DELETED"/, "AdminConsoleService must support DELETED user status filtering");

for (const [backendFile, params] of [
  ["AdminUserController.java", ["sort", "direction"]],
  ["AdminAuditController.java", ["from", "to", "sort", "direction"]],
  ["AdminSecurityController.java", ["type", "severity", "resolved", "from", "to", "sort", "direction"]],
  ["AdminSessionController.java", ["status", "device", "from", "to", "sort", "direction"]],
  ["AdminRiskController.java", ["minScore", "sort", "direction"]],
]) {
  const source = read(`../../backend/authapp/src/main/java/com/example/authapp/api/admin/${backendFile}`);
  for (const param of params) {
    if (param === "filter-options") {
      assert.match(source, /@GetMapping\("\/filter-options"\)/, `${backendFile} must expose /admin/filter-options`);
    } else {
      assert.match(
        source,
        new RegExp(`@RequestParam\\(name = "${param}"`),
        `${backendFile} must accept ${param} as an admin filter parameter`,
      );
    }
  }
}

const adminFilterOptionsController = read("../../backend/authapp/src/main/java/com/example/authapp/api/admin/AdminFilterOptionsController.java");
assert.match(
  adminFilterOptionsController,
  /@RequestMapping\("\/api\/v1\/admin"\)/,
  "AdminFilterOptionsController must use the root /api/v1/admin prefix",
);
assert.match(
  adminFilterOptionsController,
  /@GetMapping\("\/filter-options"\)/,
  "AdminFilterOptionsController must expose GET /api/v1/admin/filter-options",
);

const adminFilterOptionsResponse = read("../../backend/authapp/src/main/java/com/example/authapp/domain/admin/dto/AdminFilterOptionsResponse.java");
for (const optionGroup of [
  "userStatuses",
  "roles",
  "auditEventTypes",
  "loginStatuses",
  "incidentTypes",
  "incidentSeverities",
  "sessionStatuses",
  "riskLevels",
]) {
  assert.match(adminFilterOptionsResponse, new RegExp(optionGroup), `AdminFilterOptionsResponse must include ${optionGroup}`);
}

for (const [page, formatterPattern] of [
  ["AdminUserDetailPage", /formatSecurityDateTime/],
  ["AdminAuditLogsPage", /formatSecurityDateTime\(item\.createdAt\)/],
  ["AdminLoginHistoryPage", /formatSecurityDateTime\(item\.loginAt\)/],
  ["AdminSecurityEventsPage", /formatSecurityDateTime\(item\.createdAt\)/],
  ["AdminIncidentsPage", /formatSecurityDateTime\(item\.createdAt\)/],
  ["AdminSessionsPage", /formatSecurityDateTime\(item\.(createdAt|expiresAt|lastUsedAt)\)/],
  ["AdminRiskPage", /formatSecurityDateTime\(item\.updatedAt\)/],
]) {
  assert.match(
    read(`src/pages/admin/${page}.tsx`),
    formatterPattern,
    `${page} must display timestamps through formatSecurityDateTime`,
  );
}

for (const backendFile of [
  "AdminDashboardController.java",
  "AdminFilterOptionsController.java",
  "AdminUserController.java",
  "AdminAuditController.java",
  "AdminSecurityController.java",
  "AdminSessionController.java",
  "AdminRiskController.java",
  "AdminSettingsController.java",
]) {
  const source = read(`../../backend/authapp/src/main/java/com/example/authapp/api/admin/${backendFile}`);
  assert.match(source, /@RequestMapping\("\/api\/v1\/admin/, `${backendFile} must use /api/v1/admin prefix`);
  assert.doesNotMatch(
    source,
    /@RequestParam\((?!name\s*=|value\s*=)/,
    `${backendFile} must explicitly name every @RequestParam`,
  );
}

const adminUserControllerSource = read("../../backend/authapp/src/main/java/com/example/authapp/api/admin/AdminUserController.java");
assert.match(adminUserControllerSource, /@PostMapping\s*\n\s*public AdminUserResponse create/, "AdminUserController must expose admin user creation");
assert.match(adminUserControllerSource, /@PatchMapping\("\/\{username\}"\)/, "AdminUserController must expose admin user profile updates");
assert.match(adminUserControllerSource, /@PostMapping\("\/\{username\}\/delete"\)/, "AdminUserController must expose admin user soft delete");

for (const modelType of [
  "AdminRole",
  "AdminRoleDetail",
  "AdminRoleRequest",
  "AdminPermission",
  "AdminPermissionRequest",
  "AdminRoleAssignmentRequest",
  "AdminRoleAssignmentHistory",
]) {
  assert.match(adminModels, new RegExp(`export type ${modelType}`), `AdminModels must export ${modelType}`);
}

for (const serviceMethod of [
  "getAdminRoles",
  "getAdminRoleDetail",
  "createAdminRole",
  "updateAdminRole",
  "disableAdminRole",
  "assignAdminRolePermission",
  "removeAdminRolePermission",
  "getAdminPermissions",
  "createAdminPermission",
  "updateAdminPermission",
  "deleteAdminPermission",
  "assignAdminUserRole",
  "removeAdminUserRole",
  "getAdminRoleAssignmentHistory",
]) {
  assert.match(adminService, new RegExp(`export const ${serviceMethod} = async`), `AdminService must export ${serviceMethod}`);
}

for (const [path, pattern] of [
  ["src/pages/admin/permissions/RoleManagementPage.tsx", /getAdminRoles/],
  ["src/pages/admin/permissions/PermissionManagementPage.tsx", /getAdminPermissions/],
  ["src/pages/admin/permissions/UserPermissionAssignmentPage.tsx", /assignAdminUserRole/],
  ["src/pages/admin/permissions/GroupPermissionAssignmentPage.tsx", /assignAdminGroupRole/],
  ["src/pages/admin/account/GroupManagementPage.tsx", /getAdminRoles/],
]) {
  const source = read(path);
  assert.doesNotMatch(source, /AdminPlaceholderPage/, `${path} must be connected to the authorization API`);
  assert.match(source, pattern, `${path} must call the authorization API`);
  assert.match(source, /AdminCrudModal/, `${path} must use modal workflows for add/edit/assignment actions`);
}

assert.match(
  adminService,
  /apiClient\.post\(`\/admin\/users\/\$\{username\}\/roles`, data\)/,
  "AdminService must assign user roles through POST /admin/users/{username}/roles",
);
assert.match(
  adminService,
  /apiClient\.get<AdminRole\[\]>\(["'`]\/admin\/roles["'`]\)/,
  "AdminService must load roles from GET /admin/roles",
);
assert.match(
  adminService,
  /apiClient\.get<AdminPermission\[\]>\(["'`]\/admin\/permissions["'`]\)/,
  "AdminService must load permissions from GET /admin/permissions",
);

/* =========================================================
 * Backend SecurityController 테스트
 * ========================================================= */

// 백엔드 SecurityController 파일 읽기
const securityController = read("../../backend/authapp/src/main/java/com/example/authapp/api/SecurityController.java");

// SecurityController가 반드시 /api/v1/security를 사용해야 하는지 검사(프론트 apiClient와 경로 일관성 유지 목적)
assert.match(
  securityController,
  /@RequestMapping\("\/api\/v1\/security"\)/,
  "SecurityController must use /api/v1/security so apiClient can reach it consistently",
);

/* =========================================================
 * SecurityPage 테스트
 * ========================================================= */

/**
 * SecurityPage 컴포넌트 읽기
 */
const securityPage = read("src/pages/SecurityPage.tsx");

// 실제 보안 상태 API 호출 여부 검사 -> getSecurityStatus() 호출 필수
assert.match(
  securityPage,
  /getSecurityStatus\(/,
  "SecurityPage must call the real security status API",
);

/**
 * mock 데이터 사용 금지 검사
 *
 * setSecurity({
 *   accessTokenExpiresAt ...
 * })
 *
 * 같은 하드코딩 금지
 */
assert.doesNotMatch(
  securityPage,
  /setSecurity\(\{\s*accessTokenExpiresAt/s,
  "SecurityPage must not use mock security status data",
);

assert.match(
  securityPage,
  /formatSecurityDateTime\(security\.accessTokenExpiresAt\)/,
  "SecurityPage must format access token expiration timestamps for display",
);

assert.match(
  securityPage,
  /formatSecurityDateTime\(security\.refreshTokenExpiresAt\)/,
  "SecurityPage must format refresh token expiration timestamps for display",
);

assert.match(
  securityPage,
  /formatSecurityDateTime\(security\.lastRefreshedAt\)/,
  "SecurityPage must format last refreshed timestamps for display",
);

const dateTime = read("src/lib/dateTime.ts");
assert.match(
  dateTime,
  /formatSecurityDateTime\("2026-05-24T13:34:44\.926486"\) === "2026-05-24 13:34:44"/,
  "formatSecurityDateTime must document ISO microsecond timestamp output as YYYY-MM-DD HH:mm:ss",
);
assert.match(
  dateTime,
  /export const formatLocalDateInputValue = \(date = new Date\(\)\)/,
  "dateTime utilities must expose a local date formatter for date inputs",
);
assert.match(
  dateTime,
  /formatLocalDateInputValue\(new Date\("2026-05-25T00:30:00\+09:00"\)\) === "2026-05-25"/,
  "formatLocalDateInputValue must document that KST midnight stays on today's local date",
);

const loginHistoryPage = read("src/pages/LoginHistoryPage.tsx");
assert.match(
  loginHistoryPage,
  /formatLocalDateInputValue\(\)/,
  "LoginHistoryPage today button must use the local date formatter",
);
assert.doesNotMatch(
  loginHistoryPage,
  /toISOString\(\)\.split\("T"\)\[0\]/,
  "LoginHistoryPage must not use UTC toISOString() for the today date",
);

/* =========================================================
 * ChangePasswordPage 테스트
 * ========================================================= */

// ChangePasswordPage 파일 읽기
const changePasswordPage = read("src/pages/ChangePasswordPage.tsx");

// 실제 비밀번호 변경 API 호출 여부 검사 -> changePassword({...})
assert.match(
  changePasswordPage,
  /changePassword\(\{/,
  "ChangePasswordPage must call the real password change API",
);

// console.log로만 처리하는 fake submit 금지
assert.doesNotMatch(
  changePasswordPage,
  /console\.log\(\{\s*currentPassword/s,
  "ChangePasswordPage must not pretend to submit with console.log",
);

/* =========================================================
 * UserProfile 테스트
 * ========================================================= */

// UserProfile 페이지 읽기
const userProfile = read("src/pages/users/UserProfile.tsx");

// 실제 프로필 수정 API 호출 여부 검사 -> updateUser({...})
assert.match(
  userProfile,
  /updateUser\(\{/,
  "UserProfile must call the real profile update API",
);
