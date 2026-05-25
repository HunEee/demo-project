import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import type SecurityStatus from "@/models/SecurityStatus";
import { getSecurityStatus } from "@/services/SecurityService";
import { formatSecurityDateTime } from "@/lib/dateTime";

export default function SecurityPage() {
  const [security, setSecurity] = useState<SecurityStatus | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadSecurityStatus = async () => {
      try {
        setError("");
        setSecurity(await getSecurityStatus());
      } catch (error) {
        console.error(error);
        setError("보안 상태를 불러오지 못했습니다.");
      }
    };

    void loadSecurityStatus();
  }, []);

  if (error) {
    return <div className="text-center py-20 text-red-500">{error}</div>;
  }

  if (!security) {
    return <div className="text-center py-20">로딩 중...</div>;
  }

  const statusColor = {
    SAFE: "text-green-500",
    WARNING: "text-yellow-500",
    DANGER: "text-red-500",
  } satisfies Record<SecurityStatus["status"], string>;

  const statusText = {
    SAFE: "정상",
    WARNING: "주의",
    DANGER: "위험",
  } satisfies Record<SecurityStatus["status"], string>;

  return (
    <div className="max-w-4xl mx-auto px-6 py-10 space-y-6">
      <h1 className="text-2xl font-bold">보안 상태</h1>

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

      <div className="grid md:grid-cols-2 gap-4">
        <Card className="rounded-2xl shadow-md">
          <CardContent className="p-6">
            <p className="text-sm text-gray-500">Access Token 만료</p>
            <p className="font-medium tabular-nums">
              {formatSecurityDateTime(security.accessTokenExpiresAt)}
            </p>
          </CardContent>
        </Card>

        <Card className="rounded-2xl shadow-md">
          <CardContent className="p-6">
            <p className="text-sm text-gray-500">Refresh Token 만료</p>
            <p className="font-medium tabular-nums">
              {formatSecurityDateTime(security.refreshTokenExpiresAt)}
            </p>
          </CardContent>
        </Card>
      </div>

      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6">
          <p className="text-sm text-gray-500">최근 토큰 재발급</p>
          <p className="font-medium tabular-nums">
            {formatSecurityDateTime(security.lastRefreshedAt)}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
