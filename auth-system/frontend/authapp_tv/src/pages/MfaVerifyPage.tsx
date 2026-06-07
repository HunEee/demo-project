import { useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router";
import toast from "react-hot-toast";
import { KeyRound } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";
import useAuth from "@/auth/store";
import { verifyMfaLogin } from "@/services/MfaService";

export default function MfaVerifyPage() {
  const [params] = useSearchParams();
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);
  const logout = useAuth((state) => state.logout);
  const challengeId = params.get("challengeId") ?? "";

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!challengeId) {
      toast.error("MFA 인증 요청이 없습니다.");
      return;
    }
    if (!code.trim()) {
      toast.error("인증 코드를 입력해 주세요.");
      return;
    }

    try {
      setLoading(true);
      const response = await verifyMfaLogin(challengeId, "TOTP", code);
      if (!response.accessToken || !response.user) {
        throw new Error("로그인 완료 응답이 올바르지 않습니다.");
      }
      changeLocalLoginData(response.accessToken, response.user, true);
      toast.success("2차 인증이 완료되었습니다.");
      navigate("/dashboard", { replace: true });
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || "2차 인증에 실패했습니다.";
      toast.error(message);
      if (message.includes("모든 세션")) {
        void logout(true);
        navigate("/login", { replace: true });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <Card className="w-full max-w-md rounded-lg">
        <CardContent className="p-6">
          <div className="mb-6 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">2차 인증</h1>
              <p className="text-sm text-muted-foreground">Authenticator 앱의 6자리 코드를 입력하세요.</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="mfa-code">인증 코드</Label>
              <Input
                id="mfa-code"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={code}
                onChange={(event) => setCode(event.target.value)}
                placeholder="000000"
                maxLength={6}
              />
            </div>
            <Button type="submit" disabled={loading || !challengeId} className="w-full rounded-lg">
              {loading ? (
                <>
                  <Spinner />
                  확인 중...
                </>
              ) : (
                "인증"
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
