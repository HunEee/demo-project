import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Clock3,
  Lock,
  Settings,
  ShieldCheck,
  Users,
} from "lucide-react";
import useAuth from "@/auth/store";
import { Card, CardContent } from "@/components/ui/card";
import type { AdminDashboardSummary } from "@/models/AdminModels";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { getAdminDashboardSummary } from "@/services/AdminService";

const menu = [
  { to: "/admin/users", label: "사용자 관리", description: "계정 상태와 권한 확인", icon: Users },
  { to: "/admin/audit-logs", label: "감사 로그", description: "관리/인증 활동 추적", icon: Activity },
  { to: "/admin/login-history", label: "로그인 이력", description: "성공/실패 접속 흐름", icon: Clock3 },
  { to: "/admin/security-events", label: "보안 이벤트", description: "토큰/계정 이벤트", icon: ShieldCheck },
  { to: "/admin/incidents", label: "보안 사고", description: "미해결 사고 처리", icon: AlertTriangle },
  { to: "/admin/sessions", label: "세션/토큰", description: "활성 세션 폐기", icon: Lock },
  { to: "/admin/risk", label: "위험 사용자", description: "위험 점수 모니터링", icon: BarChart3 },
  { to: "/admin/settings", label: "운영 설정", description: "보안 정책 기준값", icon: Settings },
];

export default function AdminPage() {
  const user = useAuth((state) => state.user);
  const [summary, setSummary] = useState<AdminDashboardSummary | null>(null);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  useEffect(() => {
    if (isAdmin) {
      void getAdminDashboardSummary().then(setSummary);
    }
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  const metrics = [
    { label: "전체 사용자", value: summary?.totalUsers ?? 0, unit: "명", icon: Users },
    { label: "활성 사용자", value: summary?.activeUsers ?? 0, unit: "명", icon: ShieldCheck },
    { label: "미해결 사고", value: summary?.openIncidents ?? 0, unit: "건", icon: AlertTriangle },
    { label: "고위험 사용자", value: summary?.highRiskUsers ?? 0, unit: "건", icon: BarChart3 },
  ];

  return (
    <AdminPageShell
      title="관리자 대시보드"
      description="계정, 세션, 보안 이벤트를 한 화면에서 확인하고 운영 작업으로 바로 이동합니다."
    >
      <section className="grid gap-4 md:grid-cols-4">
        {metrics.map((metric) => {
          const Icon = metric.icon;
          return (
          <Card key={metric.label} className="rounded-lg">
            <CardContent className="flex items-start justify-between gap-4 p-5">
              <div>
                <p className="text-sm text-muted-foreground">{metric.label}</p>
                <p className="mt-2 flex items-baseline justify-center gap-1 text-3xl font-semibold tabular-nums">
                  <span>{metric.value}</span>
                  <span className="text-base font-medium text-muted-foreground">{metric.unit}</span>
                </p>
              </div>
              <span className="rounded-lg bg-muted p-2 text-muted-foreground">
                <Icon className="h-5 w-5" />
              </span>
            </CardContent>
          </Card>
          );
        })}
      </section>

      <section className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {menu.map((item) => {
          const Icon = item.icon;
          return (
            <Link key={item.to} to={item.to} className="block focus:outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
              <Card className="h-full rounded-lg transition hover:border-primary hover:bg-muted/20">
                <CardContent className="flex h-full items-start gap-3 p-5">
                  <span className="rounded-lg bg-primary/10 p-2 text-primary">
                    <Icon className="h-5 w-5" />
                  </span>
                  <span>
                    <span className="block font-medium">{item.label}</span>
                    <span className="mt-1 block text-xs leading-5 text-muted-foreground">{item.description}</span>
                  </span>
                </CardContent>
              </Card>
            </Link>
          );
        })}
      </section>
    </AdminPageShell>
  );
}
