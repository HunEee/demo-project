import { useEffect, useState } from "react";
import { Navigate, useParams } from "react-router";
import { Ban } from "lucide-react";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminUserDetail } from "@/models/AdminModels";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { AdminBadge, statusTone } from "@/pages/admin/adminUi";
import { getAdminUserDetail, revokeAdminUserTokens } from "@/services/AdminService";

export default function AdminUserDetailPage() {
  const { username = "" } = useParams();
  const user = useAuth((state) => state.user);
  const [detail, setDetail] = useState<AdminUserDetail | null>(null);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  useEffect(() => {
    if (isAdmin && username) void getAdminUserDetail(username).then(setDetail);
  }, [isAdmin, username]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell title="사용자 상세" description="계정, 최근 로그인, 보안 이벤트, 세션 정보를 확인합니다.">
      {detail ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>기본 정보</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <p className="text-lg font-semibold">{detail.user.username}</p>
                <p className="text-sm text-muted-foreground">{detail.user.email || "-"}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {detail.user.locked ? (
                  <AdminBadge tone="danger">잠금</AdminBadge>
                ) : detail.user.enabled ? (
                  <AdminBadge tone="success">활성</AdminBadge>
                ) : (
                  <AdminBadge>비활성</AdminBadge>
                )}
                <AdminBadge>{detail.user.roles?.join(", ") || "ROLE_USER"}</AdminBadge>
              </div>
              <Button variant="destructive" onClick={() => revokeAdminUserTokens(detail.user.username)}>
                <Ban className="h-4 w-4" />
                토큰 강제 만료
              </Button>
            </CardContent>
          </Card>

          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>위험 정보</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-3xl font-semibold tabular-nums">{detail.risk?.riskScore ?? 0}</p>
              <AdminBadge tone={statusTone(detail.risk?.riskLevel)}>{detail.risk?.riskLevel ?? "LOW"}</AdminBadge>
              <p className="text-sm text-muted-foreground">{detail.risk?.lastReason || "최근 위험 사유가 없습니다."}</p>
            </CardContent>
          </Card>

          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>최근 로그인</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {detail.recentLogins.slice(0, 5).map((log) => (
                <div key={log.id} className="flex items-center justify-between gap-3 border-b pb-3 last:border-0 last:pb-0">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{log.ipAddress || "-"}</p>
                    <p className="text-xs text-muted-foreground">{formatSecurityDateTime(log.loginAt)}</p>
                  </div>
                  <AdminBadge tone={statusTone(log.status)}>{log.status}</AdminBadge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="rounded-lg">
            <CardHeader>
              <CardTitle>최근 보안 이벤트</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {detail.recentEvents.slice(0, 5).map((event) => (
                <div key={event.id} className="flex items-center justify-between gap-3 border-b pb-3 last:border-0 last:pb-0">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{event.type}</p>
                    <p className="text-xs text-muted-foreground">{formatSecurityDateTime(event.createdAt)}</p>
                  </div>
                  <span className="text-xs text-muted-foreground">{event.ipAddress || "-"}</span>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      ) : null}
    </AdminPageShell>
  );
}
