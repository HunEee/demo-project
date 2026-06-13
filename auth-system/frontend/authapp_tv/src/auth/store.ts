import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";
import { loginUser, logoutUser, refreshToken } from "@/services/AuthService";
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
  restoreSession: () => Promise<void>;

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
      authLoading: true,

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

          if (loginResponseData.mfaRequired) {
            return loginResponseData;
          }

          if (!loginResponseData.accessToken || !loginResponseData.user) {
            throw new Error("로그인 응답에 인증 정보가 없습니다.");
          }

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
      // 새로고침 후 세션 복원
      // =============================
      restoreSession: async () => {
        const { accessToken, user, authStatus } = get();

        if (accessToken) {
          set({ authLoading: false });
          return;
        }

        if (!user || !authStatus) {
          set({ authLoading: false });
          return;
        }

        try {
          const response = await refreshToken("ACCESS_TOKEN_MISSING");
          if (!response.accessToken) throw new Error("토큰이 존재하지 않습니다.");

          if (get().accessToken) {
            set({ authLoading: false });
            return;
          }

          set({
            accessToken: response.accessToken,
            user: response.user ?? user,
            authStatus: true,
            authLoading: false,
          });
        } catch (error) {
          console.error("세션 복원 실패:", error);

          if (get().accessToken) {
            set({ authLoading: false });
            return;
          }

          set({
            accessToken: null,
            user: null,
            authStatus: false,
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
      version: 1,
      migrate: (persistedState) => {
        const state = persistedState as Partial<AuthState>;
        return {
          user: state.user ?? null,
          authStatus: state.authStatus ?? false,
        };
      },
      partialize: (state) => ({
        user: state.user,
        authStatus: state.authStatus,
      }),
    }

  ) 

);

export default useAuth;
