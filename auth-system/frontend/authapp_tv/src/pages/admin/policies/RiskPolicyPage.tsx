import SecurityPolicyCenterPage from "@/pages/admin/security/SecurityPolicyCenterPage";

export default function RiskPolicyPage() {
  return (
    <SecurityPolicyCenterPage
      allowedTabs={["riskDetection"]}
      title="위험탐지 정책"
      description="위험 점수 기준, 자동 대응 기준, 위험 이벤트별 규칙을 관리합니다."
    />
  );
}
