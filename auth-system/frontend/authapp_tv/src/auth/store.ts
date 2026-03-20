import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";
import { loginUser, logoutUser } from "@/services/AuthService";
import { create } from "zustand";
import { persist } from "zustand/middleware";

const LOCAL_KEY = "app_state";

// =============================
// 전역 인증 상태 타입
// =============================
type AuthState = {
  accessToken: string | null; // JWT 토큰
  user: User | null; // 사용자 정보
  authStatus: boolean; // 로그인 여부
  authLoading: boolean; // 로딩 상태

  login: (loginData: LoginData) => Promise<LoginResponseData>;
  logout: (silent?: boolean) => void;
  checkLogin: () => boolean;

  changeLocalLoginData: (
    accessToken: string,
    user: User,
    authStatus: boolean
  ) => void;
};

// =============================
// zustand 전역 인증 스토어
// =============================
const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,
      authStatus: false,
      authLoading: false,

      // 로그인 상태를 직접 변경 (토큰 재발급 등에서 사용)
      changeLocalLoginData: (accessToken, user, authStatus) => {
        set({
          accessToken,
          user,
          authStatus,
        });
      },

      // =============================
      // 로그인 처리
      // =============================
      login: async (loginData) => {
        console.log("로그인 시작...");
        set({ authLoading: true });

        try {
          const loginResponseData = await loginUser(loginData);
          console.log("로그인 응답:", loginResponseData);

          set({
            accessToken: loginResponseData.accessToken,
            user: loginResponseData.user,
            authStatus: true,
          });

          return loginResponseData;
        } catch (error) {
          console.error("로그인 실패:", error);
          throw error;
        } finally {
          set({
            authLoading: false,
          });
        }
      },

      // =============================
      // 로그아웃 처리
      // =============================
      logout: async (silent = false) => {
        try {
          set({ authLoading: true });
          // silent 모드 아닐 때만 서버 호출
          if (!silent) {
            await logoutUser();
          }
        } catch (error) {
          console.error("로그아웃 오류:", error);
        } finally {
          set({ authLoading: false });
        }

        // 상태 초기화
        set({
          accessToken: null,
          user: null,
          authLoading: false,
          authStatus: false,
        });

      },

      // =============================
      // 로그인 여부 확인
      // =============================
      checkLogin: () => {
        return !!(get().accessToken && get().authStatus);
      },

    }),

    {
      name: LOCAL_KEY, // localStorage key
    }

  ) 

);

export default useAuth;