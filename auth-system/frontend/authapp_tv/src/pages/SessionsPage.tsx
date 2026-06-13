import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Button } from "@/components/ui/button";
import { getSessions, logoutAllSessions, logoutSession } from "@/services/SessionService";
import type SessionData from "@/models/SessionData";

export default function SessionsPage() {
  const [sessions, setSessions] = useState<SessionData[]>([]);
  const [loading, setLoading] = useState(false);

  const loadSessions = async () => {
    try {
      setLoading(true);
      const data = await getSessions();
      setSessions(data);
    } catch {
      toast.error("세션 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadSessions();
  }, []);

  const sortedSessions = [...sessions].sort((a, b) => {
    if (a.current !== b.current) return a.current ? -1 : 1;
    return new Date(b.lastAccessAt).getTime() - new Date(a.lastAccessAt).getTime();
  });

  const handleLogoutSession = async (id: string) => {
    try {
      await logoutSession(id);
      toast.success("해당 세션을 로그아웃했습니다.");
      await loadSessions();
    } catch {
      toast.error("로그아웃에 실패했습니다.");
    }
  };

  const handleLogoutAll = async () => {
    try {
      await logoutAllSessions();
      toast.success("다른 기기의 세션을 모두 로그아웃했습니다.");
      await loadSessions();
    } catch {
      toast.error("전체 로그아웃에 실패했습니다.");
    }
  };

  const formatDate = (date: string) => new Date(date).toLocaleString();

  if (loading) {
    return <div className="p-6 text-center">로딩 중...</div>;
  }

  return (
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-6 flex items-center justify-between gap-4">
        <h1 className="text-2xl font-bold">세션 관리</h1>
        <Button variant="destructive" onClick={handleLogoutAll}>
          다른 기기 모두 로그아웃
        </Button>
      </div>

      <div className="space-y-4">
        {sessions.length === 0 ? (
          <div className="text-center text-gray-500">활성 세션이 없습니다.</div>
        ) : (
          sortedSessions.map((session) => (
            <div
              key={session.id}
              className={`flex items-center justify-between rounded-lg border p-4 ${
                session.current
                  ? "border-emerald-500 bg-emerald-50 dark:border-emerald-400 dark:bg-emerald-950/30"
                  : "bg-white dark:bg-gray-900"
              }`}
            >
              <div className="min-w-0">
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <p className="font-semibold">{session.current ? "현재 세션" : "다른 기기"}</p>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      session.current
                        ? "bg-emerald-600 text-white"
                        : "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-200"
                    }`}
                  >
                    {session.current ? "활성" : "로그인 중"}
                  </span>
                </div>
                <p className="text-sm text-gray-500">IP: {session.ip || "-"}</p>
                <p className="text-sm text-gray-500">기기: {session.device || "-"}</p>
                <p className="text-xs text-gray-400">생성: {formatDate(session.createdAt)}</p>
                <p className="text-xs text-gray-400">최근 접근: {formatDate(session.lastAccessAt)}</p>
              </div>

              {!session.current && (
                <Button variant="outline" onClick={() => handleLogoutSession(session.id)}>
                  로그아웃
                </Button>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
