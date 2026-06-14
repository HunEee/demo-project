import SecurityPolicyCenterPage from "@/pages/admin/security/SecurityPolicyCenterPage";

export default function SessionTokenPolicyPage() {
  return (
    <SecurityPolicyCenterPage
      allowedTabs={["sessionToken"]}
      title="세션/토큰 정책"
      description="Access Token, Refresh Token, 세션 제한, 토큰 회수 조건을 관리합니다."
    />
  );
}
