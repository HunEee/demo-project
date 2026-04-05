import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import type Session from "@/models/Session";

export default function SessionsPage() {
  const [sessions, setSessions] = useState<Session[]>([]);

  // 🔹 세션 조회
  const fetchSessions = async () => {
    const res = await fetch("/api/sessions", {
      credentials: "include",
    });
    const data = await res.json();
    setSessions(data);
  };

  // 🔹 개별 로그아웃
  const logoutSession = async (id: string) => {
    await fetch(`/api/sessions/${id}`, {
      method: "DELETE",
      credentials: "include",
    });
    fetchSessions();
  };

  // 🔹 전체 로그아웃
  const logoutAll = async () => {
    await fetch(`/api/sessions`, {
      method: "DELETE",
      credentials: "include",
    });
    fetchSessions();
  };

  useEffect(() => {
    fetchSessions();
  }, []);

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">세션 관리</h1>

      <div className="flex justify-end mb-4">
        <Button variant="destructive" onClick={logoutAll}>
          전체 로그아웃
        </Button>
      </div>

      <div className="space-y-4">
        {sessions.map((session) => (
          <div
            key={session.id}
            className="border rounded-xl p-4 flex justify-between items-center bg-white dark:bg-gray-900"
          >
            <div>
              <p className="font-semibold">
                {session.current && "🟢 현재 세션"}
              </p>
              <p className="text-sm text-gray-500">
                IP: {session.ip}
              </p>
              <p className="text-sm text-gray-500">
                기기: {session.userAgent}
              </p>
              <p className="text-xs text-gray-400">
                생성: {session.createdAt}
              </p>
              <p className="text-xs text-gray-400">
                마지막 접근: {session.lastAccessAt}
              </p>
            </div>

            {!session.current && (
              <Button
                variant="outline"
                onClick={() => logoutSession(session.id)}
              >
                로그아웃
              </Button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}