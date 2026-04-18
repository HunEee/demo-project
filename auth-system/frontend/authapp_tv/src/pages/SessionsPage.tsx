import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Button } from "@/components/ui/button";
import { getSessions, logoutSession,logoutAllSessions } from "@/services/SessionService";
import type Session from "@/models/Session";

export default function SessionsPage() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(false);

  // 세션 목록 조회
  const loadSessions = async () => {
    try {
      setLoading(true);
      const data = await getSessions();
      setSessions(data);
    } catch (e) {
      toast.error("세션 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSessions();
  }, []);

  const sortedSessions = [...sessions].sort((a, b) => {
    // 현재 세션 먼저
    if (a.current !== b.current) {
      return a.current ? -1 : 1;
    }

    // 그 다음 최근 접속 순
    return (
      new Date(b.lastAccessAt).getTime() -
      new Date(a.lastAccessAt).getTime()
    );
  });

  // 개별 로그아웃
  const handleLogoutSession = async (id: string) => {
    try {
      await logoutSession(id);
      toast.success("해당 세션이 로그아웃되었습니다.");
      await loadSessions();
    } catch {
      toast.error("로그아웃 실패");
    }
  };

  // 전체 로그아웃
  const handleLogoutAll = async () => {
    try {
      await logoutAllSessions();
      toast.success("모든 세션이 로그아웃되었습니다.");
      await loadSessions();
    } catch {
      toast.error("전체 로그아웃 실패");
    }
  };

  const formatDate = (date: string) =>
    new Date(date).toLocaleString();

  if (loading) {
    return <div className="p-6 text-center">로딩 중...</div>;
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">세션 관리</h1>

        <Button variant="destructive" onClick={handleLogoutAll}>
          전체 로그아웃
        </Button>
      </div>

      <div className="space-y-4">
        {sessions.length === 0 ? (
          <div className="text-center text-gray-500">
            활성 세션이 없습니다.
          </div>
        ) : (
          sortedSessions.map((session) => (
            <div
              key={session.id}
              className="border rounded-xl p-4 flex justify-between items-center bg-white dark:bg-gray-900"
            >
              <div>
                <p className="font-semibold">
                  {session.current ? "🟢 현재 세션" : "다른 기기"}
                </p>
                <p className="text-sm text-gray-500">
                  IP: {session.ip}
                </p>
                <p className="text-sm text-gray-500">
                  기기: {session.device}
                </p>
                <p className="text-xs text-gray-400">
                  생성: {formatDate(session.createdAt)}
                </p>
                <p className="text-xs text-gray-400">
                  마지막 접근: {formatDate(session.lastAccessAt)}
                </p>
              </div>

              {!session.current && (
                <Button
                  variant="outline"
                  onClick={() => handleLogoutSession(session.id)}
                >
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