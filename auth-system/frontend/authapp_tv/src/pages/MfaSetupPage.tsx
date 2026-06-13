import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router";
import toast from "react-hot-toast";
import { KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";
import useAuth from "@/auth/store";
import type { PreAuthTotpSetupResponse } from "@/models/MfaModels";
import { confirmPreAuthTotp, setupPreAuthTotp } from "@/services/MfaService";

export default function MfaSetupPage() {
  const [params] = useSearchParams();
  const challengeId = params.get("challengeId") ?? "";
  const [setup, setSetup] = useState<PreAuthTotpSetupResponse | null>(null);
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);

  useEffect(() => {
    if (!challengeId) {
      toast.error("MFA 등록 요청 정보가 없습니다. 다시 로그인해주세요.");
      navigate("/login", { replace: true });
      return;
    }

    void setupPreAuthTotp(challengeId)
      .then(setSetup)
      .catch((error: any) => {
        toast.error(error.response?.data?.message || "MFA 등록을 시작하지 못했습니다.");
        navigate("/login", { replace: true });
      });
  }, [challengeId, navigate]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!setup || !code.trim()) return;

    try {
      setLoading(true);
      const response = await confirmPreAuthTotp({ challengeId: setup.challengeId, methodId: setup.methodId, code });
      if (!response.accessToken || !response.user) {
        throw new Error("로그인 완료 응답이 올바르지 않습니다.");
      }
      changeLocalLoginData(response.accessToken, response.user, true);
      toast.success("MFA 등록이 완료되었습니다.");
      navigate("/dashboard", { replace: true });
    } catch (error: any) {
      toast.error(error.response?.data?.message || "인증 코드가 올바르지 않습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <Card className="w-full max-w-md rounded-lg">
        <CardContent className="space-y-5 p-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">MFA 등록</h1>
              <p className="text-sm text-muted-foreground">QR 코드를 스캔한 뒤 6자리 코드를 입력해주세요.</p>
            </div>
          </div>

          {setup ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <img src={setup.qrCodeDataUri} alt="TOTP QR" className="mx-auto h-44 w-44 rounded-lg border bg-white p-2" />
              <p className="break-all rounded-lg bg-muted p-2 font-mono text-xs">{setup.secret}</p>
              <div className="space-y-2">
                <Label htmlFor="mfa-setup-code">인증 코드</Label>
                <Input
                  id="mfa-setup-code"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  value={code}
                  onChange={(event) => setCode(event.target.value)}
                  maxLength={6}
                />
              </div>
              <Button type="submit" disabled={loading || code.trim().length === 0} className="w-full rounded-lg">
                {loading ? (
                  <>
                    <Spinner />
                    확인 중...
                  </>
                ) : (
                  "등록 완료"
                )}
              </Button>
            </form>
          ) : (
            <div className="flex justify-center py-10">
              <Spinner />
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
