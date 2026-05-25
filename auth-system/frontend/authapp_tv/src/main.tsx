import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from "react-router";
import './index.css';
import RootLayout from "./pages/RootLayout.tsx";
import App from './App.tsx';
import Login from "./pages/Login.tsx";
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
import AdminPage from './pages/AdminPage.tsx';
import AdminUsersPage from './pages/admin/AdminUsersPage.tsx';
import AdminUserDetailPage from './pages/admin/AdminUserDetailPage.tsx';
import AdminAuditLogsPage from './pages/admin/AdminAuditLogsPage.tsx';
import AdminLoginHistoryPage from './pages/admin/AdminLoginHistoryPage.tsx';
import AdminSecurityEventsPage from './pages/admin/AdminSecurityEventsPage.tsx';
import AdminIncidentsPage from './pages/admin/AdminIncidentsPage.tsx';
import AdminSessionsPage from './pages/admin/AdminSessionsPage.tsx';
import AdminRiskPage from './pages/admin/AdminRiskPage.tsx';
import AdminSettingsPage from './pages/admin/AdminSettingsPage.tsx';


createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<RootLayout />}>
        <Route index element={<App />} />
        <Route path="/login" element={<Login />} />
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
          <Route index element={<AdminPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="users/:username" element={<AdminUserDetailPage />} />
          <Route path="audit-logs" element={<AdminAuditLogsPage />} />
          <Route path="login-history" element={<AdminLoginHistoryPage />} />
          <Route path="security-events" element={<AdminSecurityEventsPage />} />
          <Route path="incidents" element={<AdminIncidentsPage />} />
          <Route path="sessions" element={<AdminSessionsPage />} />
          <Route path="risk" element={<AdminRiskPage />} />
          <Route path="settings" element={<AdminSettingsPage />} />
        </Route>

      </Route>
    </Routes>
  </BrowserRouter>
)
