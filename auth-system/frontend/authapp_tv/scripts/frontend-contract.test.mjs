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
  '<Route index element={<AdminPage />} />',
  '<Route path="users" element={<AdminUsersPage />} />',
  '<Route path="users/:username" element={<AdminUserDetailPage />} />',
  '<Route path="audit-logs" element={<AdminAuditLogsPage />} />',
  '<Route path="login-history" element={<AdminLoginHistoryPage />} />',
  '<Route path="security-events" element={<AdminSecurityEventsPage />} />',
  '<Route path="incidents" element={<AdminIncidentsPage />} />',
  '<Route path="sessions" element={<AdminSessionsPage />} />',
  '<Route path="risk" element={<AdminRiskPage />} />',
  '<Route path="settings" element={<AdminSettingsPage />} />',
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
assert.match(adminUsersPage, /item\.deleted/, "AdminUsersPage must render deleted account status separately");

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
