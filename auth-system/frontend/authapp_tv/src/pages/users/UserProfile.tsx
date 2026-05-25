import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { motion } from "framer-motion";
import useAuth from "@/auth/store";
import { deleteUser, updateUser } from "@/services/user/UserService";
import { Pencil, Shield, Mail } from "lucide-react";

function Userprofile() {
  const navigate = useNavigate();
  const user = useAuth((state) => state.user);
  const accessToken = useAuth((state) => state.accessToken);
  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);
  const logout = useAuth((state) => state.logout);

  const [isEditing, setIsEditing] = useState(false);
  const [nickname, setNickname] = useState(user?.nickname ?? "");
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    setNickname(user?.nickname ?? "");
  }, [user?.nickname]);

  const displayName = nickname || user?.username || "";

  const handleSave = async () => {
    try {
      setIsSaving(true);
      setError("");
      await updateUser({ nickname });

      if (user && accessToken) {
        changeLocalLoginData(accessToken, { ...user, nickname }, true);
      }

      setSuccess("프로필이 수정되었습니다.");
      setIsEditing(false);
    } catch (error) {
      console.error(error);
      setSuccess("");
      setError("프로필 수정에 실패했습니다.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("회원 탈퇴를 진행할까요? 이 작업은 되돌릴 수 없습니다.")) return;

    try {
      setIsDeleting(true);
      setError("");
      await deleteUser();
      logout(true);
      navigate("/", { replace: true });
    } catch (error) {
      console.error(error);
      setError("회원 탈퇴에 실패했습니다.");
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background px-6 py-10">
      <div className="max-w-4xl mx-auto space-y-8">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between"
        >
          <div>
            <h1 className="text-2xl font-bold">프로필</h1>
            <p className="text-sm text-muted-foreground">
              계정 정보를 관리하세요.
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

        <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm">
          <CardContent className="p-8">
            <div className="flex items-center gap-6 mb-8">
              <Avatar className="w-20 h-20">
                <AvatarImage src={`https://api.dicebear.com/7.x/thumbs/svg?seed=${displayName}`} />
                <AvatarFallback>{displayName.charAt(0).toUpperCase()}</AvatarFallback>
              </Avatar>

              <div>
                <h2 className="text-xl font-semibold">{displayName}</h2>
                <p className="text-sm text-muted-foreground">{user?.email}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label>닉네임</Label>
                <Input
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                  readOnly={!isEditing}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-2">
                <Label>이메일</Label>
                <Input value={user?.email ?? ""} readOnly className="rounded-xl" />
              </div>

              <div className="space-y-2">
                <Label>로그인 방식</Label>
                <div className="flex items-center gap-2 px-3 py-2 rounded-xl border text-sm text-muted-foreground">
                  <Shield size={14} /> {user?.provider ?? "LOCAL"}
                </div>
              </div>

              <div className="space-y-2">
                <Label>계정 상태</Label>
                <div className="px-3 py-2 rounded-xl border text-sm">
                  {user?.enabled ? "활성" : "비활성"}
                </div>
              </div>
            </div>

            {(error || success) && (
              <div className="mt-6 space-y-2">
                {error && <p className="text-sm text-red-500">{error}</p>}
                {success && <p className="text-sm text-green-500">{success}</p>}
              </div>
            )}

            {isEditing && (
              <div className="flex flex-col gap-3 mt-8">
                <Button className="w-full rounded-xl" onClick={handleSave} disabled={isSaving}>
                  {isSaving ? "저장 중..." : "저장"}
                </Button>
                <Button
                  variant="outline"
                  className="w-full rounded-xl"
                  onClick={() => {
                    setNickname(user?.nickname ?? "");
                    setIsEditing(false);
                    setError("");
                  }}
                  disabled={isSaving}
                >
                  취소
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="rounded-2xl border bg-card/60 backdrop-blur shadow-sm">
          <CardContent className="p-6 space-y-4">
            <h2 className="text-lg font-semibold">보안 설정</h2>

            <Button
              variant="outline"
              className="w-full flex items-center justify-center gap-2 rounded-xl"
              onClick={() => navigate("/mypage/password")}
            >
              <Mail size={16} /> 비밀번호 변경
            </Button>

            <Button
              variant="destructive"
              className="w-full rounded-xl"
              onClick={handleDelete}
              disabled={isDeleting}
            >
              {isDeleting ? "탈퇴 중..." : "회원 탈퇴"}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default Userprofile;
