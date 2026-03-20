import React, { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import { Mail, Lock, User } from "lucide-react";
import { motion } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import OAuth2Buttons from "@/components/OAuth2Buttons";
import type RegisterData from "@/models/RegisterData";
import { registerUser } from "@/services/AuthService";


function Signup() {
  const [data, setData] = useState<RegisterData>({
    name: "",
    email: "",
    password: "",
  });

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState(null);

  const navigate = useNavigate();

  // input 값 변경 처리
  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setData((value) => ({
      ...value,
      [event.target.name]: event.target.value,
    }));
  };

  // 회원가입 제출 처리
  const handleFormSubmit = async (event: FormEvent) => {
    event.preventDefault();

    // 유효성 검사
    if (data.name.trim() === "") {
      toast.error("이름을 입력해주세요.");
      return;
    }

    if (data.email.trim() === "") {
      toast.error("이메일을 입력해주세요.");
      return;
    }

    if (data.password.trim() === "") {
      toast.error("비밀번호를 입력해주세요.");
      return;
    }

    try {
      const result = await registerUser(data);
      console.log(result);

      toast.success("회원가입이 완료되었습니다.");

      setData({
        name: "",
        email: "",
        password: "",
      });

      // 로그인 페이지로 이동
      navigate("/login");
    } catch (error) {
      console.log(error);
      toast.error("회원가입 중 오류가 발생했습니다.");
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
              회원가입
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.4 }}
              className="text-center text-muted-foreground mt-2"
            >
              차세대 인증 플랫폼에 지금 가입하세요
            </motion.p>

            {/* 폼 */}
            <form onSubmit={handleFormSubmit} className="mt-8 space-y-6">
              {/* 이름 */}
              <div className="space-y-2">
                <Label htmlFor="name">이름</Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="name"
                    type="text"
                    placeholder="홍길동"
                    className="pl-10"
                    name="name"
                    value={data.name}
                    onChange={handleInputChange}
                  />
                </div>
              </div>

              {/* 이메일 */}
              <div className="space-y-2">
                <Label htmlFor="email">이메일</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="you@example.com"
                    className="pl-10"
                    name="email"
                    value={data.email}
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
                    placeholder="비밀번호를 입력하세요"
                    className="pl-10"
                    name="password"
                    value={data.password}
                    onChange={handleInputChange}
                  />
                </div>
              </div>

              <Button className="w-full rounded-2xl text-lg">
                회원가입
              </Button>

              {/* 구분선 */}
              <div className="flex items-center gap-4 my-4">
                <div className="flex-1 h-[1px] bg-border"></div>
                <span className="text-muted-foreground text-sm">또는</span>
                <div className="flex-1 h-[1px] bg-border"></div>
              </div>

              {/* 소셜 로그인 */}
              <OAuth2Buttons />
            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}

export default Signup;