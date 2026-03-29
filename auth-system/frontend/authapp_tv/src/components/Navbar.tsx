import { Button } from "./ui/button";
import { NavLink, useNavigate } from "react-router";
import useAuth from "@/auth/store";
import { motion } from "framer-motion";
import { ShieldCheck, ChevronDown } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";

export default function Navbar() {
  const navigate = useNavigate();
  const checkLogin = useAuth((state) => state.checkLogin);
  const user = useAuth((state) => state.user);
  const logout = useAuth((state) => state.logout);

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
            <span className="text-lg font-bold tracking-tight">SecureAuth</span>
            <span className="text-xs text-gray-500 dark:text-gray-400">
              인증 시스템
            </span>
          </div>
        </motion.div>

        {/* 메뉴 */}
        <div className="flex items-center gap-4">
          {/* 네비게이션 */}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-gray-100/70 dark:bg-gray-800/60 border border-gray-200 dark:border-gray-700">
            <NavLink
              to="/"
              className={({ isActive }) =>
                `text-sm font-medium px-3 py-1.5 rounded-full transition ${
                  isActive
                    ? "bg-white dark:bg-gray-900 shadow text-primary"
                    : "text-gray-600 dark:text-gray-300 hover:text-primary"
                }`
              }
            >
              홈
            </NavLink>

            {checkLogin() && (
              <NavLink
                to="/dashboard"
                className={({ isActive }) =>
                  `text-sm font-medium px-3 py-1.5 rounded-full transition ${
                    isActive
                      ? "bg-white dark:bg-gray-900 shadow text-primary"
                      : "text-gray-600 dark:text-gray-300 hover:text-primary"
                  }`
                }
              >
                대시보드
              </NavLink>
            )}
          </div>

          {/* 액션 영역 */}
          {checkLogin() ? (
            <DropdownMenu>
              <DropdownMenuTrigger>
                <button className="flex items-center gap-2 px-3 py-1.5 rounded-full border border-gray-200 dark:border-gray-700 bg-white/80 dark:bg-gray-900/60 hover:shadow transition">
                  <div className="h-8 w-8 rounded-full bg-primary/80 text-white flex items-center justify-center text-sm font-bold">
                    {user?.username?.charAt(0).toUpperCase()}
                  </div>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-200">
                    {user?.username}
                  </span>
                  <ChevronDown size={16} className="text-gray-400" />
                </button>
              </DropdownMenuTrigger>

              <DropdownMenuContent align="end" className="w-44">
                <DropdownMenuItem onClick={() => navigate("/dashboard/profile")}>
                  프로필
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => navigate("/dashboard")}>
                  대시보드
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
            <div className="flex items-center gap-2">
              <NavLink to="/login">
                {({ isActive }) => (
                  <Button
                    className={`rounded-full px-4 py-2 text-sm ${
                      isActive
                        ? "bg-black text-white dark:bg-white dark:text-black"
                        : "bg-transparent text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600"
                    }`}
                  >
                    로그인
                  </Button>
                )}
              </NavLink>

              <NavLink to="/signup">
                {({ isActive }) => (
                  <Button
                    className={`rounded-full px-4 py-2 text-sm ${
                      isActive
                        ? "bg-black text-white dark:bg-white dark:text-black"
                        : "bg-transparent text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600"
                    }`}
                  >
                    회원가입
                  </Button>
                )}
              </NavLink>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}