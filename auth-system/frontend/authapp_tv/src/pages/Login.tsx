import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import { Lock, Mail } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Alert, AlertTitle } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import OAuth2Buttons from "@/components/OAuth2Buttons";
import type LoginData from "@/models/LoginData";
import useAuth from "@/auth/store";

export default function Login() {
  const [loginData, setLoginData] = useState<LoginData>({ username: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const login = useAuth((state) => state.login);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setLoginData({ ...loginData, [event.target.name]: event.target.value });
  };

  const handleFormSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!loginData.username.trim()) {
      toast.error("아이디를 입력해 주세요.");
      return;
    }
    if (!loginData.password.trim()) {
      toast.error("비밀번호를 입력해 주세요.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      const response = await login(loginData);
      if (response.mfaRequired && response.challengeId) {
        const target = response.mfaRegistrationRequired ? "/login/mfa/setup" : "/login/mfa";
        navigate(`${target}?challengeId=${encodeURIComponent(response.challengeId)}`, { replace: true });
        return;
      }
      toast.success("로그인 성공");
      navigate("/dashboard", { replace: true });
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || "아이디 또는 비밀번호가 올바르지 않습니다.";
      toast.error(message);
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <div className="w-full max-w-md">
        <Card className="rounded-lg border-border bg-card/80 shadow-xl">
          <CardContent className="p-6">
            <h1 className="text-center text-3xl font-semibold">Welcome</h1>
            <p className="mt-2 text-center text-sm text-muted-foreground">계정으로 로그인하세요.</p>

            {error ? (
              <div className="mt-6">
                <Alert variant="destructive">
                  <AlertTitle>{error}</AlertTitle>
                </Alert>
              </div>
            ) : null}

            <form onSubmit={handleFormSubmit} className="mt-8 space-y-5">
              <div className="space-y-2">
                <Label htmlFor="username">아이디</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="username"
                    name="username"
                    value={loginData.username}
                    onChange={handleInputChange}
                    className="pl-10"
                    placeholder="아이디 입력"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">비밀번호</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="password"
                    name="password"
                    type="password"
                    value={loginData.password}
                    onChange={handleInputChange}
                    className="pl-10"
                    placeholder="비밀번호 입력"
                  />
                </div>
              </div>

              <Button type="submit" disabled={loading} className="w-full rounded-lg">
                {loading ? (
                  <>
                    <Spinner />
                    로그인 중...
                  </>
                ) : (
                  "로그인"
                )}
              </Button>

              <div className="flex items-center gap-4">
                <div className="h-px flex-1 bg-border" />
                <span className="text-sm text-muted-foreground">또는</span>
                <div className="h-px flex-1 bg-border" />
              </div>

              <OAuth2Buttons />
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
