import { useState } from "react";
import toast from "react-hot-toast";
import {BarChart3,User,ShieldCheck,Activity,Clock,} from "lucide-react";
import { motion } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import type UserT from "@/models/User";
import { getCurrentUser } from "@/services/AuthService";
import useAuth from "@/auth/store";

function Userhome() {
  const user = useAuth((state) => state.user);
  const [user1, setUser1] = useState<UserT | null>(null);

  const getUserData = async () => {
    try {
      const user1 = await getCurrentUser();
      setUser1(user1);
      toast.success("보안 API 접근 성공");
    } catch (error) {
      console.log(error);
      toast.error("데이터 조회 실패");
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground px-6 py-8">
      <div className="max-w-6xl mx-auto">
        {/* 헤더 */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-10 flex items-center justify-between"
        >
          <div>
            <h1 className="text-3xl font-bold">대시보드</h1>
            <p className="text-muted-foreground text-sm mt-1">
              {user?.username}님의 보안 및 활동 현황
            </p>
          </div>

          <Button
            onClick={getUserData}
            className="rounded-full px-6"
          >
            사용자 정보 조회
          </Button>
        </motion.div>

        {/* 통계 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
          {[
            {
              title: "총 로그인",
              value: "1,245",
              icon: <User className="w-6 h-6" />,
            },
            {
              title: "보안 점수",
              value: "98%",
              icon: <ShieldCheck className="w-6 h-6" />,
            },
            {
              title: "활성 세션",
              value: "12",
              icon: <Activity className="w-6 h-6" />,
            },
          ].map((stat, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
            >
              <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm hover:shadow-md transition">
                <CardContent className="p-5 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-muted-foreground">
                      {stat.title}
                    </p>
                    <h3 className="text-xl font-semibold mt-1">
                      {stat.value}
                    </h3>
                  </div>
                  <div className="p-2 rounded-lg bg-primary/10 text-primary">
                    {stat.icon}
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>

        {/* 최근 활동 + 사용자 정보 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* 최근 활동 */}
          <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm">
            <CardContent className="p-6">
              <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
                <BarChart3 className="w-5 h-5 text-primary" /> 최근 활동
              </h2>
              <ul className="space-y-3 text-sm text-muted-foreground">
                <li className="flex items-center gap-2">
                  <Clock className="w-4 h-4" /> Chrome에서 로그인
                </li>
                <li className="flex items-center gap-2">
                  <Clock className="w-4 h-4" /> 비밀번호 변경
                </li>
                <li className="flex items-center gap-2">
                  <Clock className="w-4 h-4" /> 새 기기 등록
                </li>
                <li className="flex items-center gap-2">
                  <Clock className="w-4 h-4" /> 모바일 로그아웃
                </li>
              </ul>
            </CardContent>
          </Card>

          {/* 사용자 정보 카드 */}
          <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm">
            <CardContent className="p-6 flex flex-col gap-4">
              <h2 className="text-lg font-semibold">사용자 정보</h2>

              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-primary text-white flex items-center justify-center font-bold">
                  {user?.username?.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="font-medium">{user?.username}</p>
                  <p className="text-sm text-muted-foreground">
                    {user1?.nickname || "닉네임 없음"}
                  </p>
                </div>
              </div>

              <Button
                onClick={getUserData}
                variant="outline"
                className="rounded-xl"
              >
                사용자 정보 새로고침
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

export default Userhome;