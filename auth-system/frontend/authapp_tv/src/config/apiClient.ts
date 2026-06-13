import useAuth from "@/auth/store";
import { refreshToken } from "@/services/AuthService";
import axios from "axios";
import toast from "react-hot-toast";

// axios 전용 API 클라이언트 (공통 설정)
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
     headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true,
    timeout: 10000,
});

// =============================
// 요청 인터셉터 (Request)
// =============================
// 모든 요청 전에 실행됨 → 토큰 자동 추가
apiClient.interceptors.request.use((config) => {
    const accessToken = useAuth.getState().accessToken;
    if (accessToken && !config.url?.includes("/jwt/refresh")) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
});

// =============================
// 토큰 재발급 관련 변수
// =============================
let isRefreshing = false; // 토큰 재발급 진행 중 여부
let pending: any[] = []; // 대기 중인 요청들

// 요청을 큐에 저장
function queueRequest(cb: any) {
  pending.push(cb);
}

// 큐에 있는 요청들 실행
function resolveQueue(newToken: string) {
  pending.forEach((cb) => cb(newToken));
  pending = [];
}

// =============================
// 응답 인터셉터 (Response)
// =============================
apiClient.interceptors.response.use(
  (response) => response,

  async (error) => {
    const original = error.config;

    const status = error.response?.status;
    const errorCode = error.response?.data?.code;
    const errorMessage = error.response?.data?.message;

    const is401 = status === 401;

    // TOKEN_EXPIRED or protected API entry without Authorization can be retried after refresh.
    // SESSION_REVOKED means the access token's session was explicitly revoked and must not be recovered.
    const isRefreshable401 = is401 && (errorCode === "TOKEN_EXPIRED" || !errorCode);

    console.log("status:", status);
    console.log("errorCode:", errorCode);
    console.log("요청 정보:", original);
    console.log("재시도 여부:", original._retry);

    if (is401 && (errorCode === "SESSION_REVOKED" || errorCode === "INVALID_TOKEN")) {
      useAuth.getState().logout(true);
      toast.error(errorMessage || "세션이 만료되었습니다. 다시 로그인해주세요.");
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
      return Promise.reject(error);
    }

    // refresh 대상이 아니면 그냥 에러 처리
    if (!isRefreshable401 || original._retry) {
      toast.error(errorMessage || "오류가 발생했습니다.");
      return Promise.reject(error);
    }

    // 재시도 표시
    original._retry = true;

    // =============================
    // 이미 refresh 중이면 대기
    // 이미 토큰 재발급 중이면 → 큐에 추가
    // =============================
    if (isRefreshing) {
      console.log("요청을 대기열에 추가");
      return new Promise((resolve, reject) => {
        queueRequest((newToken: string) => {
          if (!newToken) return reject(error);
          original.headers.Authorization = `Bearer ${newToken}`;
          resolve(apiClient(original));
        });
      });
    }

    // =============================
    // refresh 시작(토큰 재발급)
    // =============================
    isRefreshing = true;

    try {
      console.log("토큰 재발급 시작...");
      const loginResponse = await refreshToken("ACCESS_TOKEN_EXPIRED");
      const newToken = loginResponse.accessToken;
      if (!newToken) throw new Error("토큰이 존재하지 않습니다.");
      const currentUser = loginResponse.user ?? useAuth.getState().user;
      if (!currentUser) throw new Error("사용자 정보가 존재하지 않습니다.");
      
      // zustand 상태 업데이트
      useAuth.getState().changeLocalLoginData(
        newToken,
        currentUser,
        true
      );

      // 대기 중인 요청 처리
      resolveQueue(newToken);

      // 기존 요청 재시도
      original.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(original);

    } catch (error) {
      // 재발급 실패 → 전체 요청 실패 처리 + 로그아웃
      resolveQueue("");
      useAuth.getState().logout(true);
      toast.error("세션이 만료되었습니다. 다시 로그인해주세요.");
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }

  }

);

export default apiClient;
