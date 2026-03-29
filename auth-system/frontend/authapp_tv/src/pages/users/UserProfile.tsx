import { useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { motion } from "framer-motion";
import useAuth from "@/auth/store";
import { Pencil, Shield, Mail } from "lucide-react";

function Userprofile() {
  const [isEditing, setIsEditing] = useState(false);
  const user = useAuth((state) => state.user);

  return (
    <div className="min-h-screen bg-background px-6 py-10">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* 헤더 */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between"
        >
          <div>
            <h1 className="text-2xl font-bold">프로필</h1>
            <p className="text-sm text-muted-foreground">
              계정 정보를 관리하세요
            </p>
          </div>

          {!isEditing && (
            <Button
              onClick={() => setIsEditing(true)}
              className="rounded-full px-5 flex items-center gap-2"
            >
              <Pencil size={16} /> 수정
            </Button>
          )}
        </motion.div>

        {/* 프로필 카드 */}
        <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm">
          <CardContent className="p-8">
            {/* 상단 프로필 */}
            <div className="flex items-center gap-6 mb-8">
              <Avatar className="w-20 h-20">
                <AvatarImage src={`https://api.dicebear.com/7.x/thumbs/svg?seed=${user?.username}`} />
                <AvatarFallback>
                  {user?.username?.charAt(0).toUpperCase()}
                </AvatarFallback>
              </Avatar>

              <div>
                <h2 className="text-xl font-semibold">
                  {user?.username}
                </h2>
                <p className="text-sm text-muted-foreground">
                  {user?.email}
                </p>
              </div>
            </div>

            {/* 정보 영역 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label>닉네임</Label>
                <Input
                  value={user?.nickname}
                  readOnly={!isEditing}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label>이메일</Label>
                <Input
                  value={user?.email}
                  readOnly
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label>로그인 방식</Label>
                <div className="flex items-center gap-2 px-3 py-2 rounded-xl border text-sm text-muted-foreground">
                  <Shield size={14} /> {user?.provider}
                </div>
              </div>

              <div className="space-y-2">
                <Label>계정 상태</Label>
                <div className="px-3 py-2 rounded-xl border text-sm">
                  {user?.enabled ? "활성" : "비활성"}
                </div>
              </div>
            </div>

            {/* 액션 버튼 */}
            {isEditing && (
              <div className="flex flex-col gap-3 mt-8">
                <Button className="w-full rounded-xl">
                  저장
                </Button>
                <Button
                  variant="outline"
                  className="w-full rounded-xl"
                  onClick={() => setIsEditing(false)}
                >
                  취소
                </Button>

              </div>
            )}
          </CardContent>
        </Card>

        {/* 보안 설정 */}
        <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm">
          <CardContent className="p-6 space-y-4">
            <h2 className="text-lg font-semibold">보안 설정</h2>

            <Button
              variant="outline"
              className="w-full flex items-center justify-center gap-2 rounded-xl"
            >
              <Mail size={16} /> 비밀번호 변경
            </Button>

            <Button
              variant="destructive"
              className="w-full rounded-xl"
            >
              회원 탈퇴
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default Userprofile;