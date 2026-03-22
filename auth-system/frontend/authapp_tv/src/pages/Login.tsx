import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import { Mail, Lock, CheckCircle2Icon } from "lucide-react";
import { motion } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Alert, AlertTitle } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import OAuth2Buttons from "@/components/OAuth2Buttons";
import type LoginData from "@/models/LoginData";
import { loginUser } from "@/services/AuthService";
import useAuth from "@/auth/store";


function Login() {

  const [loginData, setLoginData] = useState<LoginData>({
    username: "",
    password: "",
  });

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<any>(null);

  const navigate = useNavigate();
  const login = useAuth((state) => state.login);

  // 입력값 변경 처리
  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setLoginData({
      ...loginData,
      [event.target.name]: event.target.value,
    });
  };

  // 로그인 요청 처리
  const handleFormSubmit = async (event: FormEvent) => {
    event.preventDefault();

    // 유효성 검사
    if (loginData.username.trim() === "") {
      toast.error("아이디를 입력해주세요.");
      return;
    }
    if (loginData.password.trim() === "") {
      toast.error("비밀번호를 입력해주세요.");
      return;
    }

    try {
      setLoading(true);
      // 실제 로그인 (zustand or API 연결)
      // const userInfo = await loginUser(loginData);
      await login(loginData);
      toast.success("로그인 성공 🎉");

      // 로그인 성공 시 이동
      navigate("/dashboard");

    } catch (error: any) {
      console.error(error);
      toast.error("로그인 중 오류가 발생했습니다.");
      setError(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background text-foreground px-4 py-10">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8 }}
        className="w-full max-w-md"
      >
        <Card className="bg-card/70 backdrop-blur-xl border-border shadow-2xl rounded-2xl p-6">
          <CardContent>
            {/* 제목 */}
            <motion.h1
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="text-4xl font-bold text-center"
            >
               Welcome
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.4 }}
              className="text-center text-muted-foreground mt-2"
            >
              로그인하여 서비스를 이용하세요
            </motion.p>

            {/* 에러 메시지 */}
            {error && (
              <div className="mt-6">
                <Alert variant={"destructive"}>
                  <CheckCircle2Icon />
                  <AlertTitle>
                    {error?.response
                      ? error?.response?.data?.message
                      : error?.message}
                  </AlertTitle>
                </Alert>
              </div>
            )}

            {/* 로그인 폼 */}
            <form onSubmit={handleFormSubmit} className="mt-8 space-y-6">
              {/* 아이디 */}
              <div className="space-y-2">
                <Label htmlFor="email">아이디</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="username"
                    type="username"
                    placeholder="아이디 입력"
                    className="pl-10"
                    name="username"
                    value={loginData.username}
                    onChange={handleInputChange}
                  />
                </div>
              </div>

              {/* 비밀번호 */}
              <div className="space-y-2">
                <Label htmlFor="password">비밀번호</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    className="pl-10"
                    name="password"
                    value={loginData.password}
                    onChange={handleInputChange}
                  />
                </div>
              </div>

              {/* 로그인 버튼 */}
              <Button
                type="submit"
                disabled={loading}
                className="w-full cursor-pointer rounded-2xl text-lg"
              >
                {loading ? (
                  <>
                    <Spinner />
                    로그인 중입니다...
                  </>
                ) : (
                  "로그인"
                )}
              </Button>

              {/* 구분선 */}
              <div className="flex items-center gap-4 my-4">
                <div className="flex-1 h-[1px] bg-border"></div>
                <span className="text-muted-foreground text-sm">또는</span>
                <div className="flex-1 h-[1px] bg-border"></div>
              </div>

              {/* OAuth 버튼 영역 */}
              <OAuth2Buttons />

            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}

export default Login;