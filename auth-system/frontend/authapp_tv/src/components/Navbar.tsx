import { NavLink, useNavigate } from "react-router";
import type { NavLinkProps } from "react-router";
import { ShieldCheck, ChevronDown } from "lucide-react";
import useAuth from "@/auth/store";
import { motion } from "framer-motion";
import { Button } from "./ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";

export default function Navbar() {
  const navigate = useNavigate();
  const { checkLogin, user, logout } = useAuth();

  const isLogin = checkLogin();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const navStyle: NavLinkProps["className"] = ({ isActive }) =>
    `text-sm font-medium px-3 py-1.5 rounded-full transition ${
      isActive
        ? "bg-white dark:bg-gray-900 shadow text-primary"
        : "text-gray-600 dark:text-gray-300 hover:text-primary"
    }`;
  const authBtnStyle = (isActive: boolean) =>
    `rounded-full px-4 ${
      isActive
        ? "bg-black text-white dark:bg-white dark:text-black"
        : "bg-transparent text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600"
    }`;  

  return (
    <nav className="sticky top-0 z-50 backdrop-blur-lg bg-white/70 dark:bg-black/40 border-b border-gray-200 dark:border-gray-800">
      <div className="max-w-6xl mx-auto px-6 h-20 flex items-center justify-between">
        {/* 브랜드 */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          className="flex items-center gap-3 cursor-pointer"
          onClick={() => navigate("/")}
        >
          <div className="h-10 w-10 flex items-center justify-center rounded-2xl bg-gradient-to-r from-primary to-primary/40 text-white shadow-lg">
            <ShieldCheck size={20} />
          </div>
          <div className="flex flex-col leading-tight">
            <span className="text-lg font-bold">SecureAuth</span>
            <span className="text-xs text-gray-500">인증 시스템</span>
          </div>
        </motion.div>

        {/* 메뉴 */}
        <div className="flex items-center gap-4">
          {/* 네비게이션 */}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-gray-100/70 dark:bg-gray-800/60 border border-gray-200 dark:border-gray-700">

            <NavLink to="/" className={navStyle}>홈</NavLink>

            {!isLogin && (
              <NavLink to="/password/forgot" className={navStyle}>
                비밀번호 찾기
              </NavLink>
            )}

            {isLogin && (
              <>
                <NavLink to="/dashboard" className={navStyle}>
                  대시보드
                </NavLink>

                {/* 마이페이지 드롭다운 */}
                <DropdownMenu>
                  <DropdownMenuTrigger className="text-sm font-medium px-3 py-1.5 rounded-full hover:text-primary flex items-center gap-1">
                    마이페이지 <ChevronDown size={14} />
                  </DropdownMenuTrigger>

                  <DropdownMenuContent align="start">
                    <DropdownMenuItem onClick={() => navigate("/mypage")}>
                      내 정보
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => navigate("/mypage/password")}>
                      비밀번호 변경
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => navigate("/mypage/login-history")}>
                      로그인 이력
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => navigate("/mypage/sessions")}>
                      세션 관리
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => navigate("/mypage/security")}>
                      보안 상태
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>

                {/* 관리자 */}
                {isAdmin && (
                  <DropdownMenu>
                    <DropdownMenuTrigger className="text-sm font-medium px-3 py-1.5 rounded-full hover:text-primary flex items-center gap-1">
                      관리자 <ChevronDown size={14} />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="start">
                      <DropdownMenuItem onClick={() => navigate("/admin")}>
                        대시보드
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => navigate("/admin/users")}>
                        사용자 관리
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => navigate("/admin/audit-logs")}>
                        감사 로그
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                )}

                {/* 보안 경고 (예: 상태값으로 제어) */}
                {user?.securityAlert && (
                  <NavLink to="/security/alert" className={navStyle}>
                    🚨 경고
                  </NavLink>
                )}
              </>
            )}
          </div>

          {/* 액션 영역 */}
          {isLogin ? (
            <DropdownMenu>
              <DropdownMenuTrigger >
                <button className="flex items-center gap-2 px-3 py-1.5 rounded-full border bg-white/80 dark:bg-gray-900/60 hover:shadow">
                  <div className="h-8 w-8 rounded-full bg-primary text-white flex items-center justify-center">
                    {user?.username?.charAt(0).toUpperCase()}
                  </div>
                  <span className="text-sm">{user?.username}</span>
                  <ChevronDown size={16} />
                </button>
              </DropdownMenuTrigger>

              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => navigate("/dashboard/profile")}>
                  프로필
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={() => {
                    logout();
                    navigate("/");
                  }}
                  className="text-red-500"
                >
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <div className="flex gap-2">
              <NavLink to="/login" >
                {({ isActive }) => (
                  <Button className={authBtnStyle(isActive)}>로그인</Button>
                )}
              </NavLink>
              <NavLink to="/signup">
                {({ isActive }) => (
                  <Button className={authBtnStyle(isActive)}>회원가입</Button>
                )}
              </NavLink>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}