import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import type LoginHistory from "@/models/LoginHistory";
import { getLoginHistories } from "@/services/LoginHistoryService";

// 오늘 날짜
const getToday = () => {
  return new Date().toISOString().split("T")[0];
};

export default function LoginHistoryPage() {
  const [logs, setLogs] = useState<LoginHistory[]>([]);
  const [selectedDate, setSelectedDate] = useState(getToday());

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(false);

  // 데이터 조회
  useEffect(() => {
    const fetchLogs = async () => {
      setLoading(true);
      try {
        const res = await getLoginHistories(page, 10, selectedDate);
        setLogs(res.content);
        setTotalPages(res.totalPages);
      } catch (e) {
        console.error("로그인 이력 조회 실패", e);
      } finally {
        setLoading(false);
      }
    };

    fetchLogs();
  }, [page, selectedDate]);

  // 날짜 포맷
  const formatDate = (date: string) => {
    return new Date(date).toLocaleString();
  };

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      
      {/* 헤더 + 날짜 선택 */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">로그인 이력</h1>

        <div className="flex items-center gap-2">
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => {
              setSelectedDate(e.target.value);
              setPage(0); // 날짜 바뀌면 페이지 초기화
            }}
            className="border rounded-lg px-3 py-2 text-sm"
          />

          <button
            onClick={() => {
              setSelectedDate(getToday());
              setPage(0);
            }}
            className="text-sm px-3 py-2 border rounded-lg hover:bg-gray-100"
          >
            오늘
          </button>
        </div>
      </div>

      {/* 테이블 */}
      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6 overflow-x-auto">
          <table className="w-full text-sm text-center">
            <thead className="text-gray-500 border-b">
              <tr>
                <th className="px-4 py-3">시간</th>
                <th className="px-6">IP</th>
                <th className="px-4">위치</th>
                <th className="px-6">디바이스</th>
                <th className="px-4">상태</th>
              </tr>
            </thead>

            <tbody>
              {/* 로딩 */}
              {loading && (
                <tr>
                  <td colSpan={5} className="py-6 text-gray-400">
                    로딩 중...
                  </td>
                </tr>
              )}

              {/* 데이터 */}
              {!loading &&
                logs.map((log) => (
                  <tr
                    key={log.id}
                    className="border-b hover:bg-gray-50"
                  >
                    <td className="px-4 py-3">
                      {formatDate(log.loginAt)}
                    </td>
                    <td className="px-6 py-3">{log.ip}</td>
                    <td className="px-4 py-3">{log.location || "-"}</td>
                    <td className="px-6 py-3">{log.device}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`font-medium ${
                          log.status === "FAILED"
                            ? "text-red-500"
                            : log.status === "EXPIRED"
                            ? "text-yellow-500"
                            : log.status === "LOGOUT"
                            ? "text-gray-500"
                            : "text-green-500"
                        }`}
                      >
                        {log.status === "FAILED"
                          ? "실패"
                          : log.status === "EXPIRED"
                          ? "만료"
                          : log.status === "LOGOUT"
                          ? "로그아웃"
                          : "정상"}
                      </span>
                    </td>
                  </tr>
                ))}

              {/* 빈 데이터 */}
              {!loading && logs.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-6 text-gray-400">
                    로그인 이력이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>

      {/* 페이지네이션 */}
      <div className="flex justify-center items-center gap-2 mt-6">
        
        {/* 이전 */}
        <button
          disabled={page === 0}
          onClick={() => setPage((prev) => prev - 1)}
          className="px-3 py-1 border rounded disabled:opacity-30"
        >
          이전
        </button>

        {/* 페이지 번호 */}
        {Array.from({ length: totalPages }, (_, i) => (
          <button
            key={i}
            onClick={() => setPage(i)}
            className={`px-3 py-1 border rounded ${
              page === i ? "bg-black text-white" : ""
            }`}
          >
            {i + 1}
          </button>
        ))}

        {/* 다음 */}
        <button
          disabled={page === totalPages - 1 || totalPages === 0}
          onClick={() => setPage((prev) => prev + 1)}
          className="px-3 py-1 border rounded disabled:opacity-30"
        >
          다음
        </button>

      </div>
    </div>
  );
}