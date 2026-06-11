import { useMemo, useState } from "react";
import { Download } from "lucide-react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { AdminTableCard } from "@/pages/admin/adminUi";
import { exportAdminActionLogs } from "@/services/AdminService";

const initialFilters = {
  actor: "",
  target: "",
  action: "",
  result: "",
  riskLevel: "",
  from: "",
  to: "",
};

const actionOptions = [
  { label: "전체", value: "" },
  { label: "사용자 변경", value: "UPDATE_USER" },
  { label: "계정 잠금", value: "LOCK_USER" },
  { label: "토큰 폐기", value: "TOKEN_REVOKE" },
  { label: "사고 해결", value: "RESOLVE_INCIDENT" },
  { label: "감사 로그 내보내기", value: "AUDIT_LOG_EXPORT" },
];

const resultOptions = [
  { label: "전체", value: "" },
  { label: "성공", value: "SUCCESS" },
  { label: "실패", value: "FAILED" },
  { label: "건너뜀", value: "SKIPPED" },
];

const riskOptions = [
  { label: "전체", value: "" },
  { label: "낮음", value: "LOW" },
  { label: "보통", value: "MEDIUM" },
  { label: "높음", value: "HIGH" },
  { label: "치명", value: "CRITICAL" },
];

export default function ReportsDownloadPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [filters, setFilters] = useState(initialFilters);

  const exportParams = useMemo(
    () => ({
      ...filters,
      sort: "createdAt",
      direction: "DESC",
    }),
    [filters],
  );

  const download = async () => {
    const blob = await exportAdminActionLogs(exportParams);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "감사-리포트.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="리포트 / 다운로드"
      description="관리자 작업 로그를 조건별 CSV 리포트로 다운로드합니다. 다운로드 이력은 감사 로그에 자동 기록됩니다."
      actions={
        <Button type="button" onClick={() => void download()}>
          <Download className="h-4 w-4" />
          CSV 다운로드
        </Button>
      }
    >
      <AdminFilters
        fields={[
          { name: "actor", label: "수행자", placeholder: "관리자 ID" },
          { name: "target", label: "대상", placeholder: "사용자, 대상 ID" },
          { name: "action", label: "작업", type: "select", options: actionOptions },
          { name: "result", label: "결과", type: "select", options: resultOptions },
          { name: "riskLevel", label: "위험도", type: "select", options: riskOptions },
          { name: "from", label: "시작일", type: "date" },
          { name: "to", label: "종료일", type: "date" },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void download()}
        onReset={() => setFilters(initialFilters)}
        hint="다운로드 조건"
      />

      <AdminTableCard>
        <div className="grid gap-2 p-5 text-sm text-muted-foreground">
          <p>현재 조건으로 관리자 작업 로그 CSV를 생성합니다.</p>
          <p>파일 다운로드 시 `AUDIT_LOG_EXPORT` 관리자 작업 로그와 `audit_log_exports` 이력이 함께 저장됩니다.</p>
        </div>
      </AdminTableCard>
    </AdminPageShell>
  );
}
