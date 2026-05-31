import { useState } from "react";
import type { ComponentType } from "react";
import { NavLink, useLocation, useNavigate } from "react-router";
import type { NavLinkProps } from "react-router";
import {
  ChevronDown,
  CircleAlert,
  LayoutDashboard,
  LockKeyhole,
  LogIn,
  LogOut,
  Menu,
  Settings,
  ShieldCheck,
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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type MenuItem = {
  label: string;
  path: string;
  icon: ComponentType<{ className?: string }>;
  end?: boolean;
};

const desktopLinkStyle: NavLinkProps["className"] = ({ isActive }) =>
  [
    "inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm transition",
    "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
    isActive
      ? "bg-zinc-100 font-bold text-zinc-950 shadow-sm ring-1 ring-border/70 dark:bg-zinc-800 dark:text-zinc-50"
      : "font-medium",
  ].join(" ");

const mobileLinkStyle: NavLinkProps["className"] = ({ isActive }) =>
  [
    "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition",
    "text-muted-foreground hover:bg-muted/70 hover:text-foreground",
    isActive
      ? "bg-zinc-100 font-bold text-zinc-950 shadow-sm ring-1 ring-border/70 dark:bg-zinc-800 dark:text-zinc-50"
      : "font-medium",
  ].join(" ");

const dropdownTriggerBase =
  "inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition hover:bg-muted/70 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40";

const activeDropdownTrigger =
  "bg-zinc-100 font-bold text-zinc-950 shadow-sm ring-1 ring-border/70 dark:bg-zinc-800 dark:text-zinc-50";

const dropdownItemBase = "gap-2 text-muted-foreground focus:text-foreground";
const activeDropdownItem =
  "bg-zinc-100 font-bold text-zinc-950 dark:bg-zinc-800 dark:text-zinc-50";

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { checkLogin, user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const isLogin = checkLogin();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const displayName = user?.nickname || user?.username || "사용자";
  const initial = displayName.charAt(0).toUpperCase();

  const mainItems: MenuItem[] = [
    { label: "홈", path: "/", icon: ShieldCheck, end: true },
    { label: "소개", path: "/about", icon: CircleAlert, end: true },
    ...(isLogin
      ? [
          {
            label: "대시보드",
            path: "/dashboard",
            icon: LayoutDashboard,
            end: true,
          },
        ]
      : []),
  ];

  const accountItems: MenuItem[] = [
    { label: "내 정보", path: "/mypage", icon: User, end: true },
    { label: "비밀번호 변경", path: "/mypage/password", icon: LockKeyhole },
    { label: "로그인 이력", path: "/mypage/login-history", icon: ShieldCheck },
    { label: "세션 관리", path: "/mypage/sessions", icon: LayoutDashboard },
    { label: "보안 상태", path: "/mypage/security", icon: CircleAlert },
    { label: "프로필", path: "/dashboard/profile", icon: User, end: true },
  ];

  const adminItems: MenuItem[] = [
    { label: "관리자 대시보드", path: "/admin", icon: LayoutDashboard, end: true },
    { label: "사용자 관리", path: "/admin/users", icon: Users },
    { label: "감사 로그", path: "/admin/audit-logs", icon: ShieldCheck },
    { label: "로그인 이력", path: "/admin/login-history", icon: LockKeyhole },
    { label: "보안 이벤트", path: "/admin/security-events", icon: CircleAlert },
    { label: "보안 사고", path: "/admin/incidents", icon: CircleAlert },
    { label: "세션/토큰", path: "/admin/sessions", icon: LayoutDashboard },
    { label: "위험 사용자", path: "/admin/risk", icon: Users },
    { label: "설정", path: "/admin/settings", icon: Settings },
  ];

  const isPathActive = ({ path, end }: MenuItem) =>
    end ? location.pathname === path : location.pathname.startsWith(path);

  const isAccountActive = accountItems.some(isPathActive);
  const isAdminActive = adminItems.some(isPathActive);

  const goTo = (path: string) => {
    navigate(path);
    setMobileOpen(false);
  };

  const handleLogout = () => {
    logout();
    goTo("/");
  };

  const renderDesktopDropdownItem = (item: MenuItem) => {
    const Icon = item.icon;

    return (
      <DropdownMenuItem
        key={item.path}
        onClick={() => goTo(item.path)}
        className={`${dropdownItemBase} ${
          isPathActive(item) ? activeDropdownItem : ""
        }`}
      >
        <Icon className="size-4" />
        {item.label}
      </DropdownMenuItem>
    );
  };

  const renderMobileLinks = (items: MenuItem[]) =>
    items.map(({ label, path, icon: Icon, end }) => (
      <NavLink key={path} to={path} end={end} className={mobileLinkStyle}>
        <Icon className="size-4" />
        {label}
      </NavLink>
    ));

  return (
    <nav className="sticky top-0 z-50 border-b border-border/70 bg-background/90 backdrop-blur-xl">
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
            <span className="text-xs text-muted-foreground">
              인증 보안 시스템
            </span>
          </span>
        </motion.button>

        <div className="hidden items-center gap-3 xl:flex">
          <div className="flex items-center gap-1 rounded-xl border border-border/70 bg-muted/35 p-1">
            {mainItems.map(({ label, path, icon: Icon, end }) => (
              <NavLink key={path} to={path} end={end} className={desktopLinkStyle}>
                <Icon className="size-4" />
                {label}
              </NavLink>
            ))}

            {isLogin && (
              <DropdownMenu>
                <DropdownMenuTrigger
                  className={`${dropdownTriggerBase} ${
                    isAccountActive ? activeDropdownTrigger : ""
                  }`}
                >
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
                <DropdownMenuTrigger
                  className={`${dropdownTriggerBase} ${
                    isAdminActive ? activeDropdownTrigger : ""
                  }`}
                >
                  <Settings className="size-4" />
                  관리자
                  <ChevronDown className="size-4" />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start" className="w-52">
                  {adminItems.map(renderDesktopDropdownItem)}
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
                <span className="max-w-28 truncate text-sm font-medium">
                  {displayName}
                </span>
                <ChevronDown className="size-4 text-muted-foreground" />
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-44">
                {renderDesktopDropdownItem(accountItems.at(-1)!)}
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={handleLogout}
                  className="gap-2 text-destructive focus:text-destructive"
                >
                  <LogOut className="size-4" />
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="ghost"
                className="h-10 rounded-xl px-4"
                onClick={() => goTo("/login")}
              >
                <LogIn className="size-4" />
                로그인
              </Button>
              <Button
                type="button"
                variant="outline"
                className="h-10 rounded-xl px-4"
                onClick={() => goTo("/signup")}
              >
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
          className="xl:hidden"
          onClick={() => setMobileOpen((open) => !open)}
          aria-label={mobileOpen ? "메뉴 닫기" : "메뉴 열기"}
          aria-expanded={mobileOpen}
        >
          {mobileOpen ? <X className="size-5" /> : <Menu className="size-5" />}
        </Button>

        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
            className="absolute left-3 right-3 top-[calc(100%+0.5rem)] max-h-[min(72vh,32rem)] overflow-y-auto overscroll-contain rounded-xl border border-border bg-background p-2 shadow-xl xl:hidden"
          >
            <div className="grid gap-1">{renderMobileLinks(mainItems)}</div>

            {isLogin && (
              <div className="mt-2 grid gap-1 border-t border-border pt-2">
                <p className="px-3 py-1 text-xs font-semibold text-muted-foreground">
                  계정
                </p>
                {renderMobileLinks(accountItems)}
              </div>
            )}

            {isAdmin && (
              <div className="mt-2 grid gap-1 border-t border-border pt-2">
                <p className="px-3 py-1 text-xs font-semibold text-muted-foreground">
                  관리자
                </p>
                {renderMobileLinks(adminItems)}
              </div>
            )}

            <div className="mt-2 border-t border-border pt-2">
              {isLogin ? (
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-destructive transition hover:bg-muted/70"
                >
                  <LogOut className="size-4" />
                  로그아웃
                </button>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    className="h-10 rounded-xl"
                    onClick={() => goTo("/login")}
                  >
                    <LogIn className="size-4" />
                    로그인
                  </Button>
                  <Button
                    type="button"
                    className="h-10 rounded-xl"
                    onClick={() => goTo("/signup")}
                  >
                    <UserPlus className="size-4" />
                    회원가입
                  </Button>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </div>
    </nav>
  );
}
