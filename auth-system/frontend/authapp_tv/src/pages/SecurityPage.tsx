import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import type SecurityStatus from "@/models/SecurityStatus";

export default function SecurityPage() {
  const [security, setSecurity] = useState<SecurityStatus | null>(null);

  useEffect(() => {
    // 실제 API 연결
    setSecurity({
      accessTokenExpiresAt: "2026-04-05 15:00",
      refreshTokenExpiresAt: "2026-04-06 15:00",
      lastRefreshedAt: "2026-04-05 14:00",
      status: "SAFE",
    });
  }, []);

  if (!security) {
    return <div className="text-center py-20">로딩 중...</div>;
  }

  const statusColor = {
    SAFE: "text-green-500",
    WARNING: "text-yellow-500",
    DANGER: "text-red-500",
  };

  const statusText = {
    SAFE: "정상",
    WARNING: "의심",
    DANGER: "위험",
  };

  return (
    <div className="max-w-4xl mx-auto px-6 py-10 space-y-6">
      <h1 className="text-2xl font-bold">보안 상태</h1>

      {/* 전체 상태 */}
      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6 flex justify-between items-center">
          <div>
            <p className="text-gray-500 text-sm">현재 보안 상태</p>
            <p className={`text-xl font-bold ${statusColor[security.status]}`}>
              {statusText[security.status]}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* 토큰 정보 */}
      <div className="grid md:grid-cols-2 gap-4">
        <Card className="rounded-2xl shadow-md">
          <CardContent className="p-6">
            <p className="text-sm text-gray-500">Access Token 만료</p>
            <p className="font-medium">{security.accessTokenExpiresAt}</p>
          </CardContent>
        </Card>

        <Card className="rounded-2xl shadow-md">
          <CardContent className="p-6">
            <p className="text-sm text-gray-500">Refresh Token 만료</p>
            <p className="font-medium">{security.refreshTokenExpiresAt}</p>
          </CardContent>
        </Card>
      </div>

      {/* 재발급 정보 */}
      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6">
          <p className="text-sm text-gray-500">최근 토큰 재발급</p>
          <p className="font-medium">
            {security.lastRefreshedAt || "기록 없음"}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}