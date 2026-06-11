import { useCallback, useEffect, useMemo } from "react";
import { Download } from "lucide-react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminActionLog } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminEmptyRow,
  AdminPagination,
  AdminSortableHeader,
  AdminTableCard,
  adminCellClassName,
  adminRowClassName,
  adminTableClassName,
  adminTheadClassName,
  displayValue,
  statusTone,
} from "@/pages/admin/adminUi";
import { useAdminServerList } from "@/pages/admin/useAdminList";
import { exportAdminActionLogs, getAdminActionLogs } from "@/services/AdminService";

const initialFilters = {
  actor: "",
  target: "",
  action: "",
  result: "",
  reason: "",
  riskLevel: "",
  ipAddress: "",
  userAgent: "",
  from: "",
  to: "",
};

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

const actionOptions = [
  { label: "전체", value: "" },
  { label: "사용자 생성", value: "CREATE_USER" },
  { label: "사용자 수정", value: "UPDATE_USER" },
  { label: "사용자 삭제", value: "DELETE_USER" },
  { label: "계정 잠금", value: "LOCK_USER" },
  { label: "잠금 해제", value: "UNLOCK_USER" },
  { label: "계정 비활성화", value: "DISABLE_USER" },
  { label: "계정 활성화", value: "ENABLE_USER" },
  { label: "토큰 폐기", value: "TOKEN_REVOKE" },
  { label: "비밀번호 초기화", value: "PASSWORD_RESET" },
  { label: "MFA 초기화", value: "MFA_RESET" },
  { label: "사고 해결", value: "RESOLVE_INCIDENT" },
  { label: "위험 계정 잠금", value: "RISK_MANUAL_LOCK" },
  { label: "위험 토큰 폐기", value: "RISK_TOKEN_REVOKE" },
  { label: "MFA 재등록 요구", value: "RISK_REQUIRE_MFA" },
  { label: "감사 로그 내보내기", value: "AUDIT_LOG_EXPORT" },
];

const actionLabel = (value?: string) => actionOptions.find((option) => option.value === value)?.label ?? displayValue(value);
const resultLabel = (value?: string) => resultOptions.find((option) => option.value === value)?.label ?? displayValue(value);
const riskLabel = (value?: string) => riskOptions.find((option) => option.value === value)?.label ?? displayValue(value);

function shortText(value?: string) {
  if (!value) return "-";
  return value.length > 120 ? `${value.slice(0, 120)}...` : value;
}

export default function AdminActionLogsPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const fetchPage = useCallback(
    (params: typeof initialFilters & { page: number; size: number; sort: string; direction: "ASC" | "DESC" }) =>
      getAdminActionLogs(params),
    [],
  );

  const { items, filters, pageState, sortState, load, handleFilterChange, handleSort, resetFilters } =
    useAdminServerList<AdminActionLog, typeof initialFilters>({
      initialFilters,
      initialSort: { sort: "createdAt", direction: "DESC" },
      fetchPage,
    });

  const exportParams = useMemo(
    () => ({
      ...filters,
      sort: sortState.sort,
      direction: sortState.direction,
    }),
    [filters, sortState],
  );

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin]);

  const handleExport = async () => {
    const blob = await exportAdminActionLogs(exportParams);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "관리자-작업-로그.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="관리자 작업 로그"
      description="관리자 변경 작업, 보안 대응, 다운로드 이력을 추적합니다. 민감 값은 마스킹되어 저장됩니다."
      actions={
        <Button type="button" variant="outline" className="h-9" onClick={() => void handleExport()}>
          <Download className="h-4 w-4" />
          CSV 다운로드
        </Button>
      }
    >
      <AdminFilters
        fields={[
          { name: "actor", label: "수행자", placeholder: "관리자 ID" },
          { name: "target", label: "대상", placeholder: "사용자, ID, 대상 유형" },
          { name: "action", label: "작업", type: "select", options: actionOptions },
          { name: "result", label: "결과", type: "select", options: resultOptions },
          { name: "reason", label: "사유", placeholder: "변경 사유" },
          { name: "riskLevel", label: "위험도", type: "select", options: riskOptions },
          { name: "ipAddress", label: "IP", placeholder: "IP 주소" },
          { name: "userAgent", label: "User-Agent", placeholder: "브라우저 또는 디바이스" },
          { name: "from", label: "시작일", type: "date" },
          { name: "to", label: "종료일", type: "date" },
        ]}
        values={filters}
        onChange={handleFilterChange}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <AdminTableCard>
        <table className={`${adminTableClassName} min-w-[1280px]`}>
          <thead className={adminTheadClassName}>
            <tr>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="시간" column="createdAt" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="수행자" column="actorUsername" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>대상</th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="작업" column="actionType" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>변경 전</th>
              <th className={adminCellClassName}>변경 후</th>
              <th className={adminCellClassName}>IP</th>
              <th className={adminCellClassName}>디바이스</th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="결과" column="result" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>사유</th>
              <th className={adminCellClassName}>위험도</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? <AdminEmptyRow colSpan={11} /> : null}
            {items.map((item) => (
              <tr key={item.id} className={adminRowClassName}>
                <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                  {formatSecurityDateTime(item.createdAt)}
                </td>
                <td className={adminCellClassName}>{displayValue(item.actorUsername)}</td>
                <td className={adminCellClassName}>
                  <div className="text-xs">
                    <div>{displayValue(item.targetType)}</div>
                    <div className="text-muted-foreground">{displayValue(item.targetUsername ?? item.targetId)}</div>
                  </div>
                </td>
                <td className={adminCellClassName}>
                  <AdminBadge tone="info">{actionLabel(item.actionType)}</AdminBadge>
                </td>
                <td className={`${adminCellClassName} max-w-56 text-left font-mono text-xs`}>{shortText(item.beforeValue)}</td>
                <td className={`${adminCellClassName} max-w-56 text-left font-mono text-xs`}>{shortText(item.afterValue)}</td>
                <td className={adminCellClassName}>{displayValue(item.ipAddress)}</td>
                <td className={`${adminCellClassName} max-w-48 text-xs`}>{displayValue(item.device ?? item.userAgent)}</td>
                <td className={adminCellClassName}>
                  <AdminBadge tone={statusTone(item.result)}>{resultLabel(item.result)}</AdminBadge>
                </td>
                <td className={`${adminCellClassName} max-w-52 text-left`}>{displayValue(item.reason)}</td>
                <td className={adminCellClassName}>
                  {item.riskLevel ? <AdminBadge tone={statusTone(item.riskLevel)}>{riskLabel(item.riskLevel)}</AdminBadge> : "-"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
      </AdminTableCard>
    </AdminPageShell>
  );
}
