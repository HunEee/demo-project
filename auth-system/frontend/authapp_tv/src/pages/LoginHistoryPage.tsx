import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import type LoginHistory from "@/models/LoginHistory";

export default function LoginHistoryPage() {
  const [logs, setLogs] = useState<LoginHistory[]>([]);

  useEffect(() => {
    // 실제 API 연결
    setLogs([
      {
        id: 1,
        ip: "192.168.0.1",
        userAgent: "Chrome / Windows",
        loginAt: "2026-04-05 14:22",
        location: "Seoul, KR",
      },
      {
        id: 2,
        ip: "203.0.113.5",
        userAgent: "Safari / iPhone",
        loginAt: "2026-04-04 21:10",
        location: "Busan, KR",
      },
    ]);
  }, []);

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-bold mb-6">로그인 이력</h1>

      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6 overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="text-left text-gray-500 border-b">
              <tr>
                <th className="py-3">시간</th>
                <th>IP</th>
                <th>위치</th>
                <th>디바이스</th>
                <th>상태</th>
              </tr>
            </thead>

            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="border-b hover:bg-gray-50 dark:hover:bg-gray-800">
                  <td className="py-3">{log.loginAt}</td>
                  <td>{log.ip}</td>
                  <td>{log.location || "-"}</td>
                  <td>{log.userAgent}</td>
                  <td>
                    <span className="text-green-500 font-medium">
                      정상
                    </span>
                  </td>
                </tr>
              ))}

              {logs.length === 0 && (
                <tr>
                  <td colSpan={5} className="text-center py-6 text-gray-400">
                    로그인 이력이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}