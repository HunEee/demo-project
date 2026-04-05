import useAuth from "@/auth/store";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function MyPage() {
  const user = useAuth((state) => state.user);

  if (!user) {
    return (
      <div className="flex justify-center items-center h-[60vh] text-gray-500">
        사용자 정보를 불러올 수 없습니다.
      </div>
    );
  }

  const isSocial = user.provider !== "LOCAL";

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-bold mb-6">내 정보</h1>

      {/* 프로필 카드 */}
      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6 flex flex-col md:flex-row gap-6 items-center md:items-start">
          
          {/* 프로필 이미지 */}
          <div className="h-24 w-24 rounded-full bg-primary text-white flex items-center justify-center text-2xl font-bold">
            {user.username.charAt(0).toUpperCase()}
          </div>

          {/* 정보 */}
          <div className="flex-1 w-full space-y-2">
            <div>
              <p className="text-sm text-gray-500">아이디</p>
              <p className="font-medium">{user.username}</p>
            </div>

            <div>
              <p className="text-sm text-gray-500">이메일</p>
              <p className="font-medium">{user.email}</p>
            </div>

            {user.nickname && (
              <div>
                <p className="text-sm text-gray-500">닉네임</p>
                <p className="font-medium">{user.nickname}</p>
              </div>
            )}

            <div>
              <p className="text-sm text-gray-500">로그인 방식</p>
              <p className="font-medium">
                {isSocial ? `소셜 로그인 (${user.provider})` : "일반 로그인"}
              </p>
            </div>

            <div>
              <p className="text-sm text-gray-500">계정 상태</p>
              <p className={`font-medium ${user.enabled ? "text-green-500" : "text-red-500"}`}>
                {user.enabled ? "활성화됨" : "비활성화됨"}
              </p>
            </div>
          </div>

          {/* 액션 */}
          <div className="flex flex-col gap-2 w-full md:w-auto">
            <Button onClick={() => alert("수정 기능 예정")}>
              정보 수정
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}