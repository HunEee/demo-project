import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { ShieldCheck, Trash2 } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type SecurityStatus from "@/models/SecurityStatus";
import type { MfaMethodResponse, TotpSetupResponse } from "@/models/MfaModels";
import { getSecurityStatus } from "@/services/SecurityService";
import { confirmTotp, deleteMfaMethod, getMfaMethods, setupTotp } from "@/services/MfaService";
import { formatSecurityDateTime } from "@/lib/dateTime";

export default function SecurityPage() {
  const [security, setSecurity] = useState<SecurityStatus | null>(null);
  const [methods, setMethods] = useState<MfaMethodResponse[]>([]);
  const [setup, setSetup] = useState<TotpSetupResponse | null>(null);
  const [code, setCode] = useState("");
  const [error, setError] = useState("");

  const load = async () => {
    setSecurity(await getSecurityStatus());
    setMethods(await getMfaMethods());
  };

  useEffect(() => {
    void load().catch((error) => {
      console.error(error);
      setError("보안 상태를 불러오지 못했습니다.");
    });
  }, []);

  const handleSetup = async () => {
    try {
      setSetup(await setupTotp());
      setCode("");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "TOTP 설정을 시작하지 못했습니다.");
    }
  };

  const handleConfirm = async () => {
    if (!setup) return;
    try {
      await confirmTotp(setup.methodId, code);
      toast.success("TOTP가 등록되었습니다.");
      setSetup(null);
      setCode("");
      await load();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "인증 코드가 올바르지 않습니다.");
    }
  };

  const handleDelete = async (id: number) => {
    await deleteMfaMethod(id);
    toast.success("MFA 방식이 삭제되었습니다.");
    await load();
  };

  if (error) return <div className="py-20 text-center text-red-500">{error}</div>;
  if (!security) return <div className="py-20 text-center">로딩 중...</div>;

  const enabledTotp = methods.find((method) => method.enabled && method.type === "TOTP");

  return (
    <div className="mx-auto max-w-4xl space-y-6 px-6 py-10">
      <h1 className="text-2xl font-bold">보안 상태</h1>

      <Card className="rounded-lg">
        <CardContent className="flex items-center justify-between p-6">
          <div>
            <p className="text-sm text-muted-foreground">현재 보안 상태</p>
            <p className="text-xl font-semibold">{security.status}</p>
          </div>
          <ShieldCheck className="h-6 w-6 text-emerald-600" />
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        <Card className="rounded-lg">
          <CardContent className="p-6">
            <p className="text-sm text-muted-foreground">Access Token 만료</p>
            <p className="font-medium tabular-nums">{formatSecurityDateTime(security.accessTokenExpiresAt)}</p>
          </CardContent>
        </Card>
        <Card className="rounded-lg">
          <CardContent className="p-6">
            <p className="text-sm text-muted-foreground">Refresh Token 만료</p>
            <p className="font-medium tabular-nums">{formatSecurityDateTime(security.refreshTokenExpiresAt)}</p>
          </CardContent>
        </Card>
      </div>

      <Card className="rounded-lg">
        <CardContent className="space-y-5 p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-lg font-semibold">Authenticator MFA</h2>
              <p className="text-sm text-muted-foreground">TOTP 앱으로 로그인 2차 인증을 보호합니다.</p>
            </div>
            {!enabledTotp ? (
              <Button type="button" onClick={handleSetup}>
                TOTP 등록
              </Button>
            ) : (
              <Button type="button" variant="destructive" onClick={() => handleDelete(enabledTotp.id)}>
                <Trash2 className="h-4 w-4" />
                초기화
              </Button>
            )}
          </div>

          {enabledTotp ? (
            <div className="rounded-lg border bg-muted/30 p-4 text-sm">
              <p>등록됨: {formatSecurityDateTime(enabledTotp.registeredAt)}</p>
              <p>마지막 사용: {formatSecurityDateTime(enabledTotp.lastUsedAt)}</p>
            </div>
          ) : null}

          {setup ? (
            <div className="grid gap-4 rounded-lg border p-4 md:grid-cols-[180px_1fr]">
              <img src={setup.qrCodeDataUri} alt="TOTP QR" className="h-44 w-44 rounded-lg border bg-white p-2" />
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-muted-foreground">앱에서 QR을 스캔한 뒤 6자리 코드를 입력하세요.</p>
                  <p className="mt-2 break-all rounded-lg bg-muted p-2 font-mono text-xs">{setup.secret}</p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="totp-confirm">인증 코드</Label>
                  <Input id="totp-confirm" value={code} onChange={(event) => setCode(event.target.value)} maxLength={6} />
                </div>
                <Button type="button" onClick={handleConfirm}>
                  등록 완료
                </Button>
              </div>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
