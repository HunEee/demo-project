import SecurityPolicyCenterPage from "@/pages/admin/security/SecurityPolicyCenterPage";

export default function AuthPolicyPage() {
  return (
    <SecurityPolicyCenterPage
      allowedTabs={["authentication"]}
      title="인증 정책"
      description="로그인, 비밀번호, 계정 잠금, MFA, 인증/검증 토큰 정책을 관리합니다."
    />
  );
}
