import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from "react-router";
import './index.css';
import RootLayout from "./pages/RootLayout.tsx";
import App from './App.tsx';
import Login from "./pages/Login.tsx";
import MfaVerifyPage from "./pages/MfaVerifyPage.tsx";
import MfaSetupPage from "./pages/MfaSetupPage.tsx";
import Signup from "./pages/Signup.tsx";
import About from "./pages/About.tsx";
import UserLayout from "./pages/users/UserLayout.tsx";
import UserHome from "./pages/users/UserHome.tsx";
import UserProfile from "./pages/users/UserProfile.tsx";
import MyPage from './pages/MyPage.tsx';
import LoginHistoryPage from './pages/LoginHistoryPage.tsx';
import SecurityPage from './pages/SecurityPage.tsx';
import ChangePasswordPage from './pages/ChangePasswordPage.tsx';
import SessionsPage from './pages/SessionsPage.tsx';
import OAuth2CookieCallback from './pages/OAuth2CookieCallback.tsx';
import DashboardPage from './pages/admin/status/DashboardPage.tsx';
import UserManagementPage from './pages/admin/account/UserManagementPage.tsx';
import UserDetailPage from './pages/admin/account/UserDetailPage.tsx';
import HrUserMasterPage from './pages/admin/account/HrUserMasterPage.tsx';
import ExternalUsersPage from './pages/admin/account/ExternalUsersPage.tsx';
import RoleManagementPage from './pages/admin/permissions/RoleManagementPage.tsx';
import PermissionManagementPage from './pages/admin/permissions/PermissionManagementPage.tsx';
import UserPermissionAssignmentPage from './pages/admin/permissions/UserPermissionAssignmentPage.tsx';
import GroupPermissionAssignmentPage from './pages/admin/permissions/GroupPermissionAssignmentPage.tsx';
import AdminPermissionManagementPage from './pages/admin/permissions/AdminPermissionManagementPage.tsx';
import LoginSessionManagementPage from './pages/admin/security/LoginSessionManagementPage.tsx';
import MfaManagementPage from './pages/admin/security/MfaManagementPage.tsx';
import RiskLoginDetectionPage from './pages/admin/security/RiskLoginDetectionPage.tsx';
import SecurityEventsPage from './pages/admin/security/SecurityEventsPage.tsx';
import AuthPolicyPage from './pages/admin/policies/AuthPolicyPage.tsx';
import SessionTokenPolicyPage from './pages/admin/policies/SessionTokenPolicyPage.tsx';
import RiskPolicyPage from './pages/admin/policies/RiskPolicyPage.tsx';
import OperationsPolicyPage from './pages/admin/policies/OperationsPolicyPage.tsx';
import ApplicationSsoPage from './pages/admin/integrations/ApplicationSsoPage.tsx';
import OidcClientsPage from './pages/admin/integrations/OidcClientsPage.tsx';
import ApiClientsPage from './pages/admin/integrations/ApiClientsPage.tsx';
import ServiceAccountsPage from './pages/admin/integrations/ServiceAccountsPage.tsx';
import AccessRequestsPage from './pages/admin/governance/AccessRequestsPage.tsx';
import AccessReviewsPage from './pages/admin/governance/AccessReviewsPage.tsx';
import TemporaryPermissionsPage from './pages/admin/governance/TemporaryPermissionsPage.tsx';
import PermissionExpiryPage from './pages/admin/governance/PermissionExpiryPage.tsx';
import AuditLogsPage from './pages/admin/audit/AuditLogsPage.tsx';
import AdminLoginHistoryPage from './pages/admin/audit/LoginHistoryPage.tsx';
import AdminActionLogsPage from './pages/admin/audit/AdminActionLogsPage.tsx';
import PolicyChangeHistoryPage from './pages/admin/audit/PolicyChangeHistoryPage.tsx';
import ReportsDownloadPage from './pages/admin/audit/ReportsDownloadPage.tsx';
import NotificationSettingsPage from './pages/admin/notifications/NotificationSettingsPage.tsx';
import NotificationTemplatesPage from './pages/admin/notifications/NotificationTemplatesPage.tsx';
import NotificationHistoryPage from './pages/admin/notifications/NotificationHistoryPage.tsx';


createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<RootLayout />}>
        <Route index element={<App />} />
        <Route path="/login" element={<Login />} />
        <Route path="/login/mfa" element={<MfaVerifyPage />} />
        <Route path="/login/mfa/setup" element={<MfaSetupPage />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/about" element={<About />} />
        <Route path="/cookie" element={<OAuth2CookieCallback />} />

        <Route path="/dashboard" element={<UserLayout />}>
          <Route index element={<UserHome />} />
          <Route path="profile" element={<UserProfile />} />
        </Route>

        <Route path="/mypage" element={<UserLayout />}>
          <Route index element={<MyPage />} />
          <Route path="login-history" element={<LoginHistoryPage />} />
          <Route path="security" element={<SecurityPage />} />
          <Route path="password" element={<ChangePasswordPage />} />
          <Route path="sessions" element={<SessionsPage />} />
        </Route>

        <Route path="/admin" element={<UserLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="status/dashboard" element={<DashboardPage />} />
          <Route path="account/users" element={<UserManagementPage />} />
          <Route path="account/users/:username" element={<UserDetailPage />} />
          <Route path="account/hr-users" element={<HrUserMasterPage />} />
          <Route path="account/external-users" element={<ExternalUsersPage />} />
          <Route path="permissions/roles" element={<RoleManagementPage />} />
          <Route path="permissions/permissions" element={<PermissionManagementPage />} />
          <Route path="permissions/user-assignments" element={<UserPermissionAssignmentPage />} />
          <Route path="permissions/group-assignments" element={<GroupPermissionAssignmentPage />} />
          <Route path="permissions/admin-permissions" element={<AdminPermissionManagementPage />} />
          <Route path="auth/sessions" element={<LoginSessionManagementPage />} />
          <Route path="auth/mfa" element={<MfaManagementPage />} />
          <Route path="policies/auth" element={<AuthPolicyPage />} />
          <Route path="policies/session-token" element={<SessionTokenPolicyPage />} />
          <Route path="policies/risk" element={<RiskPolicyPage />} />
          <Route path="policies/operations" element={<OperationsPolicyPage />} />
          <Route path="security/risk-logins" element={<RiskLoginDetectionPage />} />
          <Route path="security/policies" element={<AuthPolicyPage />} />
          <Route path="security/events" element={<SecurityEventsPage />} />
          <Route path="integrations/applications" element={<ApplicationSsoPage />} />
          <Route path="integrations/oidc-clients" element={<OidcClientsPage />} />
          <Route path="integrations/api-clients" element={<ApiClientsPage />} />
          <Route path="integrations/service-accounts" element={<ServiceAccountsPage />} />
          <Route path="governance/access-requests" element={<AccessRequestsPage />} />
          <Route path="governance/access-reviews" element={<AccessReviewsPage />} />
          <Route path="governance/temporary-permissions" element={<TemporaryPermissionsPage />} />
          <Route path="governance/permission-expiry" element={<PermissionExpiryPage />} />
          <Route path="audit/logs" element={<AuditLogsPage />} />
          <Route path="audit/login-history" element={<AdminLoginHistoryPage />} />
          <Route path="audit/admin-actions" element={<AdminActionLogsPage />} />
          <Route path="audit/policy-changes" element={<PolicyChangeHistoryPage />} />
          <Route path="audit/reports" element={<ReportsDownloadPage />} />
          <Route path="notifications/settings" element={<NotificationSettingsPage />} />
          <Route path="notifications/templates" element={<NotificationTemplatesPage />} />
          <Route path="notifications/history" element={<NotificationHistoryPage />} />
          <Route path="users" element={<UserManagementPage />} />
          <Route path="users/:username" element={<UserDetailPage />} />
          <Route path="audit-logs" element={<AuditLogsPage />} />
          <Route path="login-history" element={<AdminLoginHistoryPage />} />
          <Route path="security-events" element={<SecurityEventsPage />} />
          <Route path="sessions" element={<LoginSessionManagementPage />} />
          <Route path="risk" element={<RiskLoginDetectionPage />} />
        </Route>

      </Route>
    </Routes>
  </BrowserRouter>
)
