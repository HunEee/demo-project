import SecurityPolicyCenterPage from "@/pages/admin/security/SecurityPolicyCenterPage";

export default function OperationsPolicyPage() {
  return (
    <SecurityPolicyCenterPage
      allowedTabs={["operationalSecurity"]}
      title="운영 보안 정책"
      description="관리자 접근, CORS/Redirect, 로그 보관, Rate Limit 정책을 관리합니다."
    />
  );
}
