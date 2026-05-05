import React, { useState, useEffect, type FormEvent } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import { Mail, Lock, User } from "lucide-react";
import { motion } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import OAuth2Buttons from "@/components/OAuth2Buttons";
// API 관련
import { EmailCodePurpose } from "@/models/verification/EmailCodePurpose";
import { sendEmailCode, verifyEmailCode } from "@/services/verification/VerificationService";
import { signup, checkUsername } from "@/services/user/AuthService";


function Signup() {

    // const [loading, setLoading] = useState<boolean>(false);
    // const [error, setError] = useState(null);

    const [data, setData] = useState({
        nickname: "",
        email: "",
        username: "",
        password: "",
    });

    // 에러 상태
    const [errors, setErrors] = useState({
        nickname: "",
        email: "",
        username: "",
        password: "",
    });

    // 인증 관련 상태 추가
    const [verificationCode, setVerificationCode] = useState("");
    const [isCodeSent, setIsCodeSent] = useState(false);
    const [isVerified, setIsVerified] = useState(false);
    const [timer, setTimer] = useState(0);

    // username 중복 체크 상태
    const [isUsernameChecked, setIsUsernameChecked] = useState(false);
    const [isUsernameAvailable, setIsUsernameAvailable] = useState(false);

    // 검증 함수 
    const isValidNickname = (nickname: string) => /^[가-힣a-zA-Z0-9]{2,10}$/.test(nickname); // 한글/영문/숫자 2~10자
    const isValidEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);   // 이메일 형식
    const isValidUsername = (username: string) => /^[a-zA-Z0-9]{4,12}$/.test(username); // 영문/숫자 4~12자
    const isValidPassword = (password: string) => /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/.test(password); // 최소 8자, 영문, 숫자, 특수문자 포함

    // input 값 변경 처리
    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;

        setData((prev) => ({
            ...prev,
            [name]: value,
        }));

        // 실시간 검증
        let errorMsg = "";

        if (name === "nickname" && value && !isValidNickname(value)) errorMsg = "닉네임은 한글/영문/숫자 2~10자";
        if (name === "email" && value && !isValidEmail(value)) errorMsg = "올바른 이메일 형식이 아닙니다.";
        if (name === "username" && value && !isValidUsername(value)) errorMsg = "아이디는 영문/숫자 4~12자";
        if (name === "password" && value && !isValidPassword(value)) errorMsg = "비밀번호는 8자 이상 + 영문, 숫자, 특수문자 포함";

        setErrors((prev) => ({
            ...prev,
            [name]: errorMsg,
        }));

        // username 변경 시 초기화
        if (name === "username") {
            setIsUsernameChecked(false);
            setIsUsernameAvailable(false);
        }
    };
    
    const navigate = useNavigate();

    //===============================================================================================================
    // useEffect
    //===============================================================================================================

    // 타이머 로직
    useEffect(() => {
      if (timer <= 0) return;
      const interval = setInterval(() => {setTimer((prev) => prev - 1);}, 1000);
      return () => clearInterval(interval);
    }, [timer]);

    // 타이머 종료 알림
    useEffect(() => {
        if (timer === 0 && isCodeSent) {toast("인증 시간이 만료되었습니다.");}
    }, [timer]);


    //===============================================================================================================
    // API
    //===============================================================================================================

    // 인증코드 발송 API
    const sendVerificationCode = async () => {
        // 검증
        if (!data.email) return toast.error("이메일 입력");
        if (!isValidEmail(data.email)) return toast.error("이메일 형식 오류");

        try {
            await sendEmailCode({
                email: data.email,
                purpose: EmailCodePurpose.SIGNUP, // 중요
            });

            toast.success("인증코드가 발송되었습니다.");
            setIsCodeSent(true);
            setTimer(180); // 3분

        } catch (e) {
            toast.error("인증코드 발송 실패");
        }
    };

    // 인증 확인 API
    const verifyCode = async () => {
        // 검증
        if (!verificationCode) return toast.error("코드 입력");

        try {
            await verifyEmailCode({
                email: data.email,
                code: verificationCode,
                purpose: EmailCodePurpose.SIGNUP,
            });
            console.log(data.email)
            setIsVerified(true);
            setTimer(0);
            toast.success("이메일 인증 완료");

        } catch (e) {
            console.log(data.email)
            toast.error("인증코드가 올바르지 않습니다.");
        }
    };    

    // username 중복 체크 API
    const handleCheckUsername = async () => {
        if (!data.username) return toast.error("아이디 입력");
        if (!isValidUsername(data.username)) return toast.error("아이디는 영문/숫자 4~12자");

        try {
            const exists = await checkUsername({ username: data.username });

            setIsUsernameChecked(true);
            setIsUsernameAvailable(!exists);

            // 삼항으로 if문 삭제
            exists ? toast.error("이미 사용중") : toast.success("사용 가능");

        } catch (e) {
            toast.error("중복 체크 실패");
        }
    };

    // 회원가입 제출 API
    const handleFormSubmit = async (event: FormEvent) => {
        event.preventDefault();

        if (!isValidNickname(data.nickname)) return toast.error("닉네임은 2~10자 (한글/영문/숫자)");
        if (!isVerified) return toast.error("이메일 인증 필요");
        if (!isUsernameChecked || !isUsernameAvailable) return toast.error("아이디 중복 확인 필요");
        if (!isValidPassword(data.password)) return toast.error("비밀번호는 8자 이상 + 영문, 숫자, 특수문자 포함");
        
        try {
            const result = await signup({...data, verificationCode,});
            console.log(result);
            toast.success("회원가입이 완료되었습니다.");
            // 로그인 페이지 이동
            navigate("/login");
        } catch (error: any) {
            console.log(error);
            const message = error.response?.data?.message || "회원가입 중 오류가 발생했습니다.";
            toast.error(message);
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
                  <Label htmlFor="nickname">닉네임</Label>
                  <div className="relative">
                      <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                      <Input
                      id="nickname"
                      type="text"
                      placeholder="닉네임 입력"
                      className="pl-10"
                      name="nickname"
                      value={data.nickname}
                      onChange={handleInputChange}
                      />
                  </div>

                  {errors.nickname && (
                      <p className="text-red-500 text-sm">{errors.nickname}</p>
                  )}
              </div>

              {/* 이메일 + 인증 */}
              <div className="space-y-2">
                  <Label htmlFor="email">이메일</Label>

                  {/* 이메일 입력 + 버튼 */}
                  <div className="flex gap-2">
                      <div className="relative flex-1">
                          {/* 아이콘 추가*/}
                          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                          <Input
                              id="email"
                              type="email"
                              placeholder="you@example.com"
                              className="pl-10" // 아이콘 padding 맞춤
                              name="email"
                              value={data.email}
                              onChange={handleInputChange}
                              disabled={isCodeSent}
                          />
                      </div>

                      {/* 버튼 높이 input과 맞춤 */}
                      <Button 
                          type="button" 
                          onClick={sendVerificationCode}
                          className="w-25 whitespace-nowrap"
                      >
                          {isCodeSent ? "재발송" : "인증코드 발송"}
                      </Button>
                  </div>

                  {/* 이메일 에러 메시지 */}
                  {errors.email && (
                    <p className="text-sm text-red-500">{errors.email}</p>
                  )}

                  {/* 타이머 스타일 개선 */}
                  {isCodeSent && (
                      <p className="text-sm text-muted-foreground">
                          남은 시간: {Math.floor(timer / 60)}:
                          {String(timer % 60).padStart(2, "0")}
                      </p>
                  )}

                  {/* 인증코드 입력 */}
                  {isCodeSent && (
                      <div className="flex gap-2">
                          <div className="relative flex-1">
                              {/* 아이콘 추가*/}
                              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                              <Input
                                  placeholder="인증코드 입력"
                                  className="pl-10"
                                  value={verificationCode}
                                  onChange={(e) => setVerificationCode(e.target.value)}
                                  disabled={isVerified}
                              />
                          </div>

                          <Button type="button" onClick={verifyCode} className="w-25">
                              인증
                          </Button>
                      </div>
                  )}

                  {/* 인증 완료 표시 */}
                  {isVerified && (
                      <p className="text-green-500 text-sm flex items-center gap-1">
                          ✔ 인증 완료
                      </p>
                  )}
              </div>

              {/* 아이디 */}
              <div className="space-y-2">
                  <Label htmlFor="username">아이디</Label>

                  <div className="flex gap-2">
                      <div className="relative flex-1">
                          <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                          <Input
                              id="username"
                              type="text"
                              placeholder="아이디 입력"
                              className="pl-10"
                              name="username"
                              value={data.username}
                              onChange={(e) => {
                                  handleInputChange(e);
                                  setIsUsernameChecked(false); // 값 바뀌면 다시 체크해야됨
                              }}
                          />
                      </div>

                      <Button
                          type="button"
                          onClick={handleCheckUsername}
                          disabled={!isValidUsername(data.username)} // 형식 안맞으면 비활성화
                          className="w-25 whitespace-nowrap"
                      >
                          중복 확인
                      </Button>
                  </div>

                  {/* username 에러 */}
                  {errors.username && (
                    <p className="text-sm text-red-500">{errors.username}</p>
                  )}

                  {/* 상태 표시 */}
                  {isUsernameChecked && (
                      <p className={`text-sm ${isUsernameAvailable ? "text-green-500" : "text-red-500"}`}>
                          {isUsernameAvailable ? "✔ 사용 가능" : "✖ 이미 사용중"}
                      </p>
                  )}
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

                {/* password 에러 */}
                {errors.password && (
                  <p className="text-sm text-red-500">{errors.password}</p>
                )}

              </div>

              <Button type="submit" className="w-full rounded-2xl text-lg">
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