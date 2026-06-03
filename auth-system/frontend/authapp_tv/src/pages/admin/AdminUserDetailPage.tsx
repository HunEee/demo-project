import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Navigate, useParams } from "react-router";
import { Ban, KeyRound, ShieldOff } from "lucide-react";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminUserDetail } from "@/models/AdminModels";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { AdminBadge, statusTone } from "@/pages/admin/adminUi";
import {
  getAdminUserDetail,
  resetAdminUserMfa,
  resetAdminUserPassword,
  revokeAdminUserTokens,
} from "@/services/AdminService";

type DetailTab = "profile" | "roles" | "sessions" | "logins" | "audit" | "risk";

const tabs: Array<{ id: DetailTab; label: string }> = [
  { id: "profile", label: "기본 정보" },
  { id: "roles", label: "역할/그룹" },
  { id: "sessions", label: "세션" },
  { id: "logins", label: "로그인 이력" },
  { id: "audit", label: "감사 로그" },
  { id: "risk", label: "위험 정보" },
];

const display = (value?: string | null) => value || "-";

export default function AdminUserDetailPage() {
  const { username = "" } = useParams();
  const user = useAuth((state) => state.user);
  const [detail, setDetail] = useState<AdminUserDetail | null>(null);
  const [activeTab, setActiveTab] = useState<DetailTab>("profile");
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const load = async () => {
    if (username) setDetail(await getAdminUserDetail(username));
  };

  useEffect(() => {
    if (isAdmin && username) void load().catch(() => undefined);
  }, [isAdmin, username]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell title="사용자 상세" description="계정, 역할, 세션, 로그인 이력, 감사 로그, 위험 정보를 확인합니다.">
      {detail ? (
        <div className="space-y-4">
          <Card className="rounded-lg">
            <CardContent className="flex flex-col gap-4 p-5 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p className="text-xl font-semibold">{detail.user.username}</p>
                <p className="text-sm text-muted-foreground">{display(detail.user.email)}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <AdminBadge tone={statusTone(detail.user.status)}>{detail.user.status ?? "ACTIVE"}</AdminBadge>
                  <AdminBadge>{detail.user.userType ?? "INTERNAL"}</AdminBadge>
                  <AdminBadge>{detail.user.authMethod ?? "PASSWORD"}</AdminBadge>
                  <AdminBadge tone={detail.user.mfaEnabled ? "success" : "default"}>{detail.user.mfaEnabled ? "MFA ON" : "MFA OFF"}</AdminBadge>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  onClick={async () => {
                    const result = await resetAdminUserPassword(detail.user.username);
                    window.alert(`임시 비밀번호: ${result.temporaryPassword}`);
                    await load();
                  }}
                >
                  <KeyRound className="h-4 w-4" />
                  비밀번호 초기화
                </Button>
                <Button
                  variant="outline"
                  onClick={async () => {
                    await resetAdminUserMfa(detail.user.username);
                    await load();
                  }}
                >
                  <ShieldOff className="h-4 w-4" />
                  MFA 초기화
                </Button>
                <Button
                  variant="destructive"
                  onClick={async () => {
                    await revokeAdminUserTokens(detail.user.username);
                    await load();
                  }}
                >
                  <Ban className="h-4 w-4" />
                  토큰 강제 만료
                </Button>
              </div>
            </CardContent>
          </Card>

          <div className="flex flex-wrap gap-2">
            {tabs.map((tab) => (
              <Button
                key={tab.id}
                type="button"
                size="sm"
                variant={activeTab === tab.id ? "default" : "outline"}
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
              </Button>
            ))}
          </div>

          {activeTab === "profile" ? (
            <Card className="rounded-lg">
              <CardHeader>
                <CardTitle>기본 정보</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-4 text-sm md:grid-cols-2 lg:grid-cols-3">
                <Info label="이름" value={detail.user.name ?? detail.user.nickname} />
                <Info label="이메일" value={detail.user.email} />
                <Info label="사번" value={detail.user.employeeNo} />
                <Info label="부서" value={detail.user.department} />
                <Info label="직급" value={detail.user.position} />
                <Info label="고용형태" value={detail.user.employmentType} />
                <Info label="마지막 로그인" value={detail.user.lastLoginAt ? formatSecurityDateTime(detail.user.lastLoginAt) : undefined} />
              </CardContent>
            </Card>
          ) : null}

          {activeTab === "roles" ? (
            <Card className="rounded-lg">
              <CardHeader>
                <CardTitle>역할/그룹</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <p className="mb-2 text-xs font-medium text-muted-foreground">역할</p>
                  <div className="flex flex-wrap gap-2">
                    {(detail.user.roles?.length ? detail.user.roles : ["ROLE_USER"]).map((role) => (
                      <AdminBadge key={role}>{role}</AdminBadge>
                    ))}
                  </div>
                </div>
                <div>
                  <p className="mb-2 text-xs font-medium text-muted-foreground">그룹</p>
                  <div className="flex flex-wrap gap-2">
                    {detail.groups.length === 0 ? <span className="text-sm text-muted-foreground">소속 그룹 없음</span> : null}
                    {detail.groups.map((group) => (
                      <AdminBadge key={group.id}>{group.name}</AdminBadge>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          ) : null}

          {activeTab === "sessions" ? (
            <ListCard title="세션">
              {detail.sessions.map((session) => (
                <Row key={session.id} title={session.device || session.jti} meta={`${session.ipAddress || "-"} · ${session.expiresAt ? formatSecurityDateTime(session.expiresAt) : "-"}`}>
                  <AdminBadge tone={session.revoked ? "danger" : "success"}>{session.revoked ? "REVOKED" : "ACTIVE"}</AdminBadge>
                </Row>
              ))}
            </ListCard>
          ) : null}

          {activeTab === "logins" ? (
            <ListCard title="로그인 이력">
              {detail.recentLogins.map((log) => (
                <Row key={log.id} title={log.ipAddress || "-"} meta={log.loginAt ? formatSecurityDateTime(log.loginAt) : "-"}>
                  <AdminBadge tone={statusTone(log.status)}>{log.status}</AdminBadge>
                </Row>
              ))}
            </ListCard>
          ) : null}

          {activeTab === "audit" ? (
            <ListCard title="감사 로그">
              {[...detail.adminActions, ...detail.recentEvents].map((event) => (
                <Row
                  key={`${"actionType" in event ? "admin" : "auth"}-${event.id}`}
                  title={"actionType" in event ? event.actionType : event.type}
                  meta={"actorUsername" in event ? `${event.actorUsername} -> ${event.targetUsername}` : event.description || "-"}
                >
                  <span className="text-xs text-muted-foreground">{event.createdAt ? formatSecurityDateTime(event.createdAt) : "-"}</span>
                </Row>
              ))}
            </ListCard>
          ) : null}

          {activeTab === "risk" ? (
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
          ) : null}
        </div>
      ) : null}
    </AdminPageShell>
  );
}

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 font-medium">{display(value)}</p>
    </div>
  );
}

function ListCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Card className="rounded-lg">
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">{children}</CardContent>
    </Card>
  );
}

function Row({ title, meta, children }: { title: string; meta: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 border-b pb-3 last:border-0 last:pb-0">
      <div className="min-w-0">
        <p className="truncate text-sm font-medium">{title}</p>
        <p className="text-xs text-muted-foreground">{meta}</p>
      </div>
      {children}
    </div>
  );
}
