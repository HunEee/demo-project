import { useState } from "react";
import type { ComponentType } from "react";
import { createPortal } from "react-dom";
import { NavLink, useLocation, useNavigate } from "react-router";
import type { NavLinkProps } from "react-router";
import {
  Bell,
  ChevronDown,
  ChevronRight,
  CircleAlert,
  ClipboardCheck,
  FileText,
  KeyRound,
  LayoutDashboard,
  Link2,
  LogIn,
  LogOut,
  Menu,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  User,
  UserPlus,
  Users,
  X,
} from "lucide-react";
import { motion } from "framer-motion";
import useAuth from "@/auth/store";
import { Button } from "./ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type MenuItem = {
  label: string;
  path: string;
  icon?: ComponentType<{ className?: string }>;
  end?: boolean;
};

type MenuGroup = {
  label: string;
  icon: ComponentType<{ className?: string }>;
  items: MenuItem[];
};

const adminGroups: MenuGroup[] = [
  {
    label: "현황",
    icon: LayoutDashboard,
    items: [{ label: "대시보드", path: "/admin/status/dashboard", icon: LayoutDashboard, end: true }],
  },
  {
    label: "계정 관리",
    icon: Users,
    items: [
      { label: "사용자 관리", path: "/admin/account/users", icon: Users },
      { label: "HR 기준정보 관리", path: "/admin/account/hr-users", icon: UserPlus },
      { label: "외부 사용자 관리", path: "/admin/account/external-users", icon: UserPlus },
    ],
  },
  {
    label: "권한 관리",
    icon: KeyRound,
    items: [
      { label: "역할 관리", path: "/admin/permissions/roles", icon: ShieldCheck },
      { label: "권한 관리", path: "/admin/permissions/permissions", icon: KeyRound },
      { label: "사용자 권한 할당", path: "/admin/permissions/user-assignments", icon: User },
      { label: "그룹 권한 할당", path: "/admin/permissions/group-assignments", icon: Users },
      { label: "관리자 권한 관리", path: "/admin/permissions/admin-permissions", icon: ShieldCheck },
    ],
  },
  {
    label: "인증 관리",
    icon: ShieldCheck,
    items: [
      { label: "로그인 / 세션 관리", path: "/admin/auth/sessions", icon: LayoutDashboard },
      { label: "MFA 관리", path: "/admin/auth/mfa", icon: ShieldCheck },
      { label: "인증 정책 관리", path: "/admin/auth/policies", icon: SlidersHorizontal },
    ],
  },
  {
    label: "보안 관리",
    icon: CircleAlert,
    items: [
      { label: "보안 이벤트 관리", path: "/admin/security/events", icon: FileText },
      { label: "위험 로그인 탐지", path: "/admin/security/risk-logins", icon: CircleAlert },
      { label: "관리자 IP 제한", path: "/admin/security/admin-ip", icon: ShieldCheck },
    ],
  },
  {
    label: "감사 / 모니터링",
    icon: FileText,
    items: [
      { label: "감사 로그", path: "/admin/audit/logs", icon: FileText },
      { label: "관리자 작업 로그", path: "/admin/audit/admin-actions", icon: ClipboardCheck },
      { label: "로그인 이력", path: "/admin/audit/login-history", icon: ShieldCheck },
      { label: "정책 변경 이력", path: "/admin/audit/policy-changes", icon: SlidersHorizontal },
      { label: "리포트 / 다운로드", path: "/admin/audit/reports", icon: FileText },
    ],
  },
  {
    label: "애플리케이션 연동",
    icon: Link2,
    items: [
      { label: "애플리케이션 / SSO 관리", path: "/admin/integrations/applications", icon: Link2 },
      { label: "OAuth2 / OIDC 클라이언트 관리", path: "/admin/integrations/oidc-clients", icon: ShieldCheck },
      { label: "API 클라이언트 관리", path: "/admin/integrations/api-clients", icon: KeyRound },
      { label: "서비스 계정 관리", path: "/admin/integrations/service-accounts", icon: User },
    ],
  },
  {
    label: "접근 거버넌스",
    icon: ClipboardCheck,
    items: [
      { label: "접근 요청 / 승인 관리", path: "/admin/governance/access-requests", icon: ClipboardCheck },
      { label: "권한 검토 / 접근 리뷰", path: "/admin/governance/access-reviews", icon: ShieldCheck },
      { label: "임시 권한 관리", path: "/admin/governance/temporary-permissions", icon: KeyRound },
      { label: "권한 만료 / 회수 관리", path: "/admin/governance/permission-expiry", icon: CircleAlert },
    ],
  },
  {
    label: "알림 관리",
    icon: Bell,
    items: [
      { label: "알림 설정", path: "/admin/notifications/settings", icon: Bell },
      { label: "알림 템플릿 관리", path: "/admin/notifications/templates", icon: FileText },
      { label: "발송 이력", path: "/admin/notifications/history", icon: ClipboardCheck },
    ],
  },
  {
    label: "시스템 설정",
    icon: Settings,
    items: [
      { label: "시스템 설정", path: "/admin/system/settings", icon: Settings },
      { label: "CORS / Redirect URI 설정", path: "/admin/system/cors-redirect", icon: Link2 },
      { label: "로그 보관 정책", path: "/admin/system/log-retention", icon: FileText },
    ],
  },
];

const desktopLinkStyle: NavLinkProps["className"] = ({ isActive }) =>
  [
    "inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm transition",
    "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
    isActive ? "bg-zinc-100 font-bold text-zinc-950 shadow-sm ring-1 ring-border/70 dark:bg-zinc-800 dark:text-zinc-50" : "font-medium",
  ].join(" ");

const dropdownTriggerBase =
  "inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition hover:bg-muted/70 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40";
const activeDropdownTrigger = "bg-zinc-100 font-bold text-zinc-950 shadow-sm ring-1 ring-border/70 dark:bg-zinc-800 dark:text-zinc-50";
const dropdownItemBase = "gap-2 text-muted-foreground focus:text-foreground";
const activeDropdownItem = "bg-zinc-100 font-bold text-zinc-950 dark:bg-zinc-800 dark:text-zinc-50";

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { checkLogin, user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [expandedMobileGroup, setExpandedMobileGroup] = useState<string | null>(null);

  const isLogin = checkLogin();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const displayName = user?.nickname || user?.username || "사용자";
  const initial = displayName.charAt(0).toUpperCase();

  const mainItems: MenuItem[] = [
    { label: "홈", path: "/", icon: ShieldCheck, end: true },
    { label: "소개", path: "/about", icon: CircleAlert, end: true },
    ...(isLogin ? [{ label: "대시보드", path: "/dashboard", icon: LayoutDashboard, end: true }] : []),
  ];

  const accountItems: MenuItem[] = [
    { label: "내 정보", path: "/mypage", icon: User, end: true },
    { label: "비밀번호 변경", path: "/mypage/password", icon: KeyRound },
    { label: "로그인 이력", path: "/mypage/login-history", icon: ShieldCheck },
    { label: "세션 관리", path: "/mypage/sessions", icon: LayoutDashboard },
    { label: "보안 상태", path: "/mypage/security", icon: CircleAlert },
    { label: "프로필", path: "/dashboard/profile", icon: User, end: true },
  ];

  const isPathActive = ({ path, end }: MenuItem) => (end ? location.pathname === path : location.pathname.startsWith(path));
  const isAccountActive = accountItems.some(isPathActive);
  const isAdminActive = location.pathname.startsWith("/admin");

  const goTo = (path: string) => {
    navigate(path);
    setMobileOpen(false);
  };

  const handleLogout = () => {
    logout();
    goTo("/");
  };

  const renderDesktopDropdownItem = (item: MenuItem) => {
    const Icon = item.icon ?? FileText;

    return (
      <DropdownMenuItem
        key={item.path}
        onClick={() => goTo(item.path)}
        className={`${dropdownItemBase} ${isPathActive(item) ? activeDropdownItem : ""}`}
      >
        <Icon className="size-4" />
        {item.label}
      </DropdownMenuItem>
    );
  };

  const renderMobileItem = (item: MenuItem) => {
    const Icon = item.icon ?? FileText;
    const active = isPathActive(item);

    return (
      <button
        key={item.path}
        type="button"
        onClick={() => goTo(item.path)}
        className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-semibold transition ${
          active ? "bg-muted text-foreground" : "text-foreground hover:bg-muted/70"
        }`}
      >
        <Icon className="size-4 shrink-0 text-muted-foreground" />
        <span className="min-w-0 flex-1 truncate">{item.label}</span>
      </button>
    );
  };

  const renderMobileGroup = (group: MenuGroup) => {
    const Icon = group.icon;
    const expanded = expandedMobileGroup === group.label;
    const active = group.items.some(isPathActive);

    return (
      <div key={group.label} className="border-b border-border/70 py-1">
        <button
          type="button"
          className={`flex w-full items-center gap-3 px-1 py-3 text-left text-lg font-bold tracking-normal ${
            active ? "text-primary" : "text-foreground"
          }`}
          onClick={() => setExpandedMobileGroup(expanded ? null : group.label)}
        >
          <Icon className="size-5 shrink-0 text-muted-foreground" />
          <span className="min-w-0 flex-1 truncate">{group.label}</span>
          <span className="h-7 border-l border-border" />
          <ChevronRight className={`size-5 shrink-0 transition ${expanded ? "rotate-90" : ""}`} />
        </button>
        {expanded ? <div className="grid gap-1 pb-2 pl-7">{group.items.map(renderMobileItem)}</div> : null}
      </div>
    );
  };

  return (
    <>
    <nav className="sticky top-0 z-50 border-b border-border/70 bg-background/95 backdrop-blur-xl">
      <div className="relative mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <motion.button
          type="button"
          initial={{ opacity: 0, x: -16 }}
          animate={{ opacity: 1, x: 0 }}
          className="group flex shrink-0 items-center gap-3 rounded-lg text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
          onClick={() => goTo("/")}
          aria-label="SecureAuth 홈으로 이동"
        >
          <span className="flex size-10 items-center justify-center rounded-xl bg-foreground text-background shadow-sm transition group-hover:scale-105">
            <ShieldCheck className="size-5" />
          </span>
          <span className="flex flex-col leading-tight">
            <span className="text-base font-semibold">SecureAuth</span>
            <span className="text-xs text-muted-foreground">인증 보안 시스템</span>
          </span>
        </motion.button>

        <div className="hidden items-center gap-3 lg:flex">
          <div className="flex items-center gap-1 rounded-xl border border-border/70 bg-muted/35 p-1">
            {mainItems.map(({ label, path, icon: Icon = FileText, end }) => (
              <NavLink key={path} to={path} end={end} className={desktopLinkStyle}>
                <Icon className="size-4" />
                {label}
              </NavLink>
            ))}

            {isLogin && (
              <DropdownMenu>
                <DropdownMenuTrigger className={`${dropdownTriggerBase} ${isAccountActive ? activeDropdownTrigger : ""}`}>
                  <User className="size-4" />
                  마이페이지
                  <ChevronDown className="size-4" />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start" className="w-48">
                  {accountItems.map(renderDesktopDropdownItem)}
                </DropdownMenuContent>
              </DropdownMenu>
            )}

            {isAdmin && (
              <DropdownMenu>
                <DropdownMenuTrigger className={`${dropdownTriggerBase} ${isAdminActive ? activeDropdownTrigger : ""}`}>
                  <Settings className="size-4" />
                  관리자
                  <ChevronDown className="size-4" />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start" className="w-64">
                  {adminGroups.map((group) => {
                    const Icon = group.icon;
                    const active = group.items.some(isPathActive);
                    return (
                      <DropdownMenuSub key={group.label}>
                        <DropdownMenuSubTrigger className={active ? activeDropdownItem : ""}>
                          <Icon className="size-4" />
                          {group.label}
                        </DropdownMenuSubTrigger>
                        <DropdownMenuSubContent className="w-64">
                          {group.items.map(renderDesktopDropdownItem)}
                        </DropdownMenuSubContent>
                      </DropdownMenuSub>
                    );
                  })}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>

          {isLogin ? (
            <DropdownMenu>
              <DropdownMenuTrigger className="flex shrink-0 items-center gap-2 rounded-xl border border-border bg-background px-2 py-1.5 transition hover:bg-muted/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40">
                <span className="flex size-8 items-center justify-center rounded-lg bg-foreground text-sm font-semibold text-background">
                  {initial}
                </span>
                <span className="max-w-28 truncate text-sm font-medium">{displayName}</span>
                <ChevronDown className="size-4 text-muted-foreground" />
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-44">
                <DropdownMenuItem onClick={() => goTo("/dashboard/profile")} className={dropdownItemBase}>
                  <User className="size-4" />
                  프로필
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout} className="gap-2 text-destructive focus:text-destructive">
                  <LogOut className="size-4" />
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <div className="flex items-center gap-2">
              <Button type="button" variant="ghost" className="h-10 rounded-xl px-4" onClick={() => goTo("/login")}>
                <LogIn className="size-4" />
                로그인
              </Button>
              <Button type="button" variant="outline" className="h-10 rounded-xl px-4" onClick={() => goTo("/signup")}>
                <UserPlus className="size-4" />
                회원가입
              </Button>
            </div>
          )}
        </div>

        <Button
          type="button"
          variant="outline"
          size="icon-lg"
          className="lg:hidden"
          onClick={() => setMobileOpen(true)}
          aria-label="전체 메뉴 열기"
          aria-expanded={mobileOpen}
        >
          <Menu className="size-5" />
        </Button>
      </div>

    </nav>

    {mobileOpen
      ? createPortal(
        <div className="fixed inset-0 z-[9999] isolate lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-black/40"
            onClick={() => setMobileOpen(false)}
            aria-label="전체 메뉴 닫기"
          />
          <motion.aside
            initial={{ x: "100%" }}
            animate={{ x: 0 }}
            exit={{ x: "100%" }}
            transition={{ duration: 0.24, ease: "easeOut" }}
            className="absolute right-0 top-0 z-10 flex h-dvh w-[min(92vw,390px)] flex-col border-l border-border bg-white shadow-2xl dark:bg-zinc-950"
          >
            <div className="flex h-16 items-center justify-between border-b px-5">
              <div className="min-w-0">
                <p className="text-xs font-medium text-muted-foreground">SecureAuth</p>
                <p className="truncate text-lg font-bold text-foreground">전체 메뉴</p>
              </div>
              <button
                type="button"
                className="flex size-10 shrink-0 items-center justify-center rounded-full hover:bg-muted"
                onClick={() => setMobileOpen(false)}
                aria-label="전체 메뉴 닫기"
              >
                <X className="size-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-3">
              <div className="border-b border-border/70 pb-2">{mainItems.map(renderMobileItem)}</div>

              {isLogin ? (
                <div className="mt-2">{renderMobileGroup({ label: "마이페이지", icon: User, items: accountItems })}</div>
              ) : null}

              {isAdmin ? (
                <div className="mt-3">
                  <p className="mb-1 px-1 text-xs font-semibold text-muted-foreground">관리자</p>
                  {adminGroups.map(renderMobileGroup)}
                </div>
              ) : null}
            </div>

            <div className="border-t p-4">
              {isLogin ? (
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-base font-semibold text-destructive hover:bg-muted"
                >
                  <LogOut className="size-4" />
                  로그아웃
                </button>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <Button type="button" variant="outline" className="h-11 rounded-xl" onClick={() => goTo("/login")}>
                    <LogIn className="size-4" />
                    로그인
                  </Button>
                  <Button type="button" className="h-11 rounded-xl" onClick={() => goTo("/signup")}>
                    <UserPlus className="size-4" />
                    회원가입
                  </Button>
                </div>
              )}
            </div>
          </motion.aside>
        </div>,
        document.body,
      )
      : null}
    </>
  );
}
