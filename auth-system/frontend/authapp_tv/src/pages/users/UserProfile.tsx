import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { motion } from "framer-motion";
import useAuth from "@/auth/store";

function Userprofile() {
  const [isEditing, setIsEditing] = useState(false);
  const user = useAuth((state) => state.user);

  return (
    <div className="p-6 max-w-3xl mx-auto space-y-8">
      {/* 제목 */}
      <motion.h1
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-3xl font-bold text-center"
      >
        사용자 프로필
      </motion.h1>

      {/* 프로필 카드 */}
      <Card className="rounded-2xl shadow-md p-6">
        <CardHeader>
          <CardTitle className="text-xl font-semibold">
            프로필 정보
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* 아바타 */}
          <div className="flex flex-col items-center gap-3">
            <Avatar className="w-28 h-28 border shadow-md">
              <AvatarImage src="https://api.dicebear.com/7.x/thumbs/svg?seed=user" />
              <AvatarFallback>U</AvatarFallback>
            </Avatar>
            <Button variant="outline" className="rounded-xl px-5">
              프로필 사진 변경
            </Button>
          </div>

          {/* 사용자 정보 */}
          {!isEditing ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name">이름</Label>
                <Input
                  id="name"
                  value={user?.nickname}
                  readOnly
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">이메일</Label>
                <Input
                  id="email"
                  value={user?.email}
                  readOnly
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="provider">로그인 방식</Label>
                <Input
                  id="provider"
                  value={user?.provider}
                  readOnly
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="enabled">계정 상태</Label>
                <Input
                  id="enabled"
                  value={user?.enabled ? "활성" : "비활성"}
                  readOnly
                  className="rounded-xl"
                />
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name">이름</Label>
                <Input
                  id="name"
                  value={user?.nickname}
                  onChange={(e) => {}}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">이메일</Label>
                <Input
                  id="email"
                  value={user?.email}
                  readOnly
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="provider">로그인 방식</Label>
                <Input
                  id="provider"
                  value={user?.provider}
                  readOnly
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="enabled">계정 상태</Label>
                <Input
                  id="enabled"
                  value={user?.enabled ? "활성" : "비활성"}
                  readOnly
                  className="rounded-xl"
                />
              </div>
            </div>
          )}

          {/* 버튼 */}
          {!isEditing ? (
            <Button
              onClick={() => setIsEditing(true)}
              className="w-full rounded-2xl mt-4 text-lg"
            >
              프로필 수정
            </Button>
          ) : (
            <div className="flex gap-3 mt-4">
              <Button
                className="rounded-2xl w-full"
                onClick={() => setIsEditing(false)}
              >
                취소
              </Button>
              <Button
                className="rounded-2xl w-full"
                onClick={() => {
                  /* 저장 로직 */
                }}
              >
                저장
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 계정 설정 */}
      <Card className="rounded-2xl shadow-md p-6">
        <CardHeader>
          <CardTitle className="text-xl">계정 설정</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button
            variant="outline"
            className="w-full rounded-xl py-3 text-base"
          >
            비밀번호 변경
          </Button>
          <Button
            variant="destructive"
            className="w-full rounded-xl py-3 text-base"
          >
            회원 탈퇴
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

export default Userprofile;