import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import { Save } from "lucide-react";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AdminSettings } from "@/models/AdminModels";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { getAdminSettings, updateAdminSettings } from "@/services/AdminService";

const fields: Array<{ key: keyof AdminSettings; label: string; description: string }> = [
  { key: "maxLoginFailures", label: "로그인 실패 허용 횟수", description: "초과 시 계정 보호 조치 기준입니다." },
  { key: "highRiskThreshold", label: "고위험 기준", description: "고위험 사용자로 분류되는 점수입니다." },
  { key: "criticalRiskThreshold", label: "치명 위험 기준", description: "강제 로그아웃 등 즉시 대응 기준입니다." },
  { key: "sessionExpireDays", label: "세션 만료 일수", description: "refresh token 세션의 기본 유지 기간입니다." },
];

export default function AdminSettingsPage() {
  const user = useAuth((state) => state.user);
  const [settings, setSettings] = useState<AdminSettings | null>(null);
  const isAdmin = hasAdminAccess(user);

  useEffect(() => {
    if (isAdmin) void getAdminSettings().then(setSettings).catch(() => undefined);
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  const setNumber = (key: keyof AdminSettings, value: string) => {
    if (!settings) return;
    setSettings({ ...settings, [key]: Number(value) });
  };

  return (
    <AdminPageShell title="관리자 설정" description="보안 운영 정책의 기준값을 조정합니다.">
      {settings ? (
        <Card className="mx-auto w-full max-w-3xl rounded-lg">
          <CardContent className="space-y-5 p-5">
            {fields.map((field) => (
              <div key={field.key} className="grid gap-2 sm:grid-cols-[1fr_180px] sm:items-center">
                <div>
                  <Label htmlFor={field.key}>{field.label}</Label>
                  <p className="mt-1 text-xs leading-5 text-muted-foreground">{field.description}</p>
                </div>
                <Input
                  id={field.key}
                  type="number"
                  value={settings[field.key] as number}
                  onChange={(event) => setNumber(field.key, event.target.value)}
                />
              </div>
            ))}

            <label className="flex items-center justify-between gap-4 rounded-lg border bg-muted/20 p-4 text-sm">
              <span>
                <span className="block font-medium">치명 위험 시 강제 로그아웃</span>
                <span className="mt-1 block text-xs leading-5 text-muted-foreground">
                  임계치 이상 위험 사용자의 세션을 즉시 종료합니다.
                </span>
              </span>
              <input
                className="h-5 w-5"
                type="checkbox"
                checked={settings.forceLogoutOnCriticalRisk}
                onChange={(event) => setSettings({ ...settings, forceLogoutOnCriticalRisk: event.target.checked })}
              />
            </label>

            <div className="flex justify-end">
              <Button onClick={async () => setSettings(await updateAdminSettings(settings))}>
                <Save className="h-4 w-4" />
                저장
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : null}
    </AdminPageShell>
  );
}
