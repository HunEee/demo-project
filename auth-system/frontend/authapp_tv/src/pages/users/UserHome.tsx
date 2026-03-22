import { useState } from "react";
import toast from "react-hot-toast";
import { BarChart3, User, ShieldCheck, Activity } from "lucide-react";
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
      toast.success("보안 API에 정상적으로 접근했습니다.");
    } catch (error) {
      console.log(error);
      toast.error("데이터를 가져오는 중 오류가 발생했습니다.");
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground p-6">
      {/* 페이지 제목 */}
      <motion.h1
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="text-4xl font-bold mb-8"
      >
        대시보드 개요
      </motion.h1>

      {/* 통계 영역 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
        {[
          {
            title: "총 로그인 횟수",
            value: "1,245",
            icon: <User className="w-8 h-8 text-primary" />,
          },
          {
            title: "보안 점수",
            value: "98%",
            icon: <ShieldCheck className="w-8 h-8 text-primary" />,
          },
          {
            title: "활성 세션",
            value: "12",
            icon: <Activity className="w-8 h-8 text-primary" />,
          },
        ].map((stat, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: i * 0.1 }}
          >
            <Card className="bg-card/70 backdrop-blur-lg border-border rounded-2xl shadow-lg">
              <CardContent className="p-6 flex items-center gap-4">
                <div className="p-3 bg-muted rounded-xl">{stat.icon}</div>
                <div>
                  <p className="text-muted-foreground text-sm">{stat.title}</p>
                  <h3 className="text-2xl font-semibold">{stat.value}</h3>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>

      {/* 활동 내역 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6, delay: 0.2 }}
      >
        <Card className="bg-card/70 backdrop-blur-lg border-border rounded-2xl shadow-lg mb-10">
          <CardContent className="p-6">
            <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
              <BarChart3 className="w-6 h-6 text-primary" /> 최근 활동
            </h2>
            <ul className="space-y-3 text-muted-foreground">
              <li>• Chrome(Windows)에서 로그인</li>
              <li>• 비밀번호 변경</li>
              <li>• 새로운 기기를 신뢰 목록에 추가</li>
              <li>• Safari(iPhone)에서 로그아웃</li>
            </ul>
          </CardContent>
        </Card>
      </motion.div>

      {/* 버튼 영역 */}
      <div className="text-center">
        <Button onClick={getUserData} className="rounded-2xl px-8 text-lg">
          현재 사용자 정보 가져오기
        </Button>

        <p>{user1?.nickname}</p>
      </div>
    </div>
  );
}

export default Userhome;