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
    if (accessToken) {
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
    const is401 = error.response.status === 401;
    const original = error.config;

    console.log("요청 정보:", original);
    console.log("재시도 여부:", original._retry);

    // 401이 아니거나 이미 재시도한 요청이면 에러 처리
    if (!is401 || original._retry) {
      if (error.response && error.response.data) {
        toast.error(error.response.data?.message || "오류가 발생했습니다.");
      }
      console.error("API 에러:", error.response.data);
      console.error("전체 에러:", error);
      return Promise.reject(error);
    }

    // 재시도 표시
    original._retry = true;

    // 이미 토큰 재발급 중이면 → 큐에 추가
    if (isRefreshing) {
      console.log("요청을 대기열에 추가");

      return new Promise((resolve, reject) => {
        queueRequest((newToken: string) => {
          if (!newToken) return reject();
          original.headers.Authorization = `Bearer ${newToken}`;
          resolve(apiClient(original));
        });
      });
    }

    // =============================
    // 토큰 재발급 시작
    // =============================
    isRefreshing = true;

    try {
      console.log("토큰 재발급 시작...");
      const loginResponse = await refreshToken();
      const newToken = loginResponse.accessToken;
      if (!newToken) throw new Error("토큰이 존재하지 않습니다.");
      
      // zustand 상태 업데이트
      useAuth.getState().changeLocalLoginData(
        loginResponse.accessToken,
        loginResponse.user,
        true
      );

      // 대기 중인 요청 처리
      resolveQueue(newToken);

      // 기존 요청 재시도
      original.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(original);

    } catch (error) {
      // 재발급 실패 → 전체 요청 실패 처리 + 로그아웃
      resolveQueue("null");
      useAuth.getState().logout();
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }

  }

);

export default apiClient;