import { useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function ChangePasswordPage() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const validate = () => {
    if (!currentPassword || !newPassword || !confirmPassword) {
      return "모든 항목을 입력해주세요.";
    }

    if (newPassword.length < 8) {
      return "비밀번호는 8자 이상이어야 합니다.";
    }

    if (newPassword !== confirmPassword) {
      return "새 비밀번호가 일치하지 않습니다.";
    }

    return "";
  };

  const handleSubmit = () => {
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      setSuccess("");
      return;
    }

    // API 연결
    console.log({
      currentPassword,
      newPassword,
    });

    setError("");
    setSuccess("비밀번호가 성공적으로 변경되었습니다.");

    // 입력값 초기화
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
  };

  return (
    <div className="max-w-md mx-auto px-6 py-10">
      <h1 className="text-2xl font-bold mb-6">비밀번호 변경</h1>

      <Card className="rounded-2xl shadow-md">
        <CardContent className="p-6 space-y-4">

          {/* 현재 비밀번호 */}
          <div>
            <p className="text-sm text-gray-500 mb-1">현재 비밀번호</p>
            <Input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
          </div>

          {/* 새 비밀번호 */}
          <div>
            <p className="text-sm text-gray-500 mb-1">새 비밀번호</p>
            <Input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>

          {/* 새 비밀번호 확인 */}
          <div>
            <p className="text-sm text-gray-500 mb-1">새 비밀번호 확인</p>
            <Input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {/* 에러 메시지 */}
          {error && (
            <p className="text-sm text-red-500">{error}</p>
          )}

          {/* 성공 메시지 */}
          {success && (
            <p className="text-sm text-green-500">{success}</p>
          )}

          {/* 버튼 */}
          <Button className="w-full" onClick={handleSubmit}>
            변경하기
          </Button>

        </CardContent>
      </Card>
    </div>
  );
}