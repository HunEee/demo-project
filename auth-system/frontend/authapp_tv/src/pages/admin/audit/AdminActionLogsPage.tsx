import { useCallback, useEffect, useMemo, useState } from "react";
import { Download, Eye } from "lucide-react";
import { Navigate } from "react-router";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import type { AdminActionLog } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminDateTimeCell from "@/pages/admin/AdminDateTimeCell";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminCrudModal,
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
  { label: "세션 강제 종료", value: "REVOKE_SESSION" },
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

function prettyJson(value?: string) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function userAgentText(item: AdminActionLog) {
  return displayValue(item.userAgent ?? item.device);
}

export default function AdminActionLogsPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = hasAdminAccess(user);
  const [selectedLog, setSelectedLog] = useState<AdminActionLog | null>(null);

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
  }, [isAdmin, load]);

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
        <table className={`${adminTableClassName} min-w-[860px]`}>
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
              <th className={adminCellClassName}>
                <AdminSortableHeader label="결과" column="result" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>위험도</th>
              <th className={adminCellClassName}>상세</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
            {items.map((item) => (
              <tr key={item.id} className={adminRowClassName}>
                <td className={adminCellClassName}>
                  <AdminDateTimeCell value={item.createdAt} />
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
                <td className={adminCellClassName}>
                  <AdminBadge tone={statusTone(item.result)}>{resultLabel(item.result)}</AdminBadge>
                </td>
                <td className={adminCellClassName}>
                  {item.riskLevel ? <AdminBadge tone={statusTone(item.riskLevel)}>{riskLabel(item.riskLevel)}</AdminBadge> : "-"}
                </td>
                <td className={adminCellClassName}>
                  <Button type="button" variant="ghost" size="sm" onClick={() => setSelectedLog(item)}>
                    <Eye className="h-4 w-4" />
                    상세
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
      </AdminTableCard>

      <AdminCrudModal
        open={selectedLog !== null}
        title="감사 로그 상세"
        description="전후 데이터, 메타데이터, 요청 정보를 확인합니다."
        contentClassName="sm:max-w-[760px]"
        onOpenChange={(open) => {
          if (!open) setSelectedLog(null);
        }}
      >
        {selectedLog ? (
          <div className="grid gap-4 text-sm">
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <p className="text-xs text-muted-foreground">수행자</p>
                <p className="mt-1 font-medium">{displayValue(selectedLog.actorUsername)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">작업</p>
                <p className="mt-1 font-medium">{actionLabel(selectedLog.actionType)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">대상</p>
                <p className="mt-1 font-medium">
                  {displayValue(selectedLog.targetType)} / {displayValue(selectedLog.targetUsername ?? selectedLog.targetId)}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">결과</p>
                <p className="mt-1 font-medium">{resultLabel(selectedLog.result)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">IP</p>
                <p className="mt-1 font-medium">{displayValue(selectedLog.ipAddress)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">User-Agent</p>
                <p className="mt-1 break-all font-medium">{userAgentText(selectedLog)}</p>
              </div>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">사유</p>
              <p className="mt-1 rounded border bg-muted/20 p-3">{displayValue(selectedLog.reason)}</p>
            </div>
            <div className="grid gap-3 lg:grid-cols-2">
              <div>
                <p className="text-xs text-muted-foreground">변경 전</p>
                <pre className="mt-1 max-h-64 overflow-auto rounded border bg-muted/30 p-3 text-xs">
                  {prettyJson(selectedLog.beforeValue)}
                </pre>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">변경 후</p>
                <pre className="mt-1 max-h-64 overflow-auto rounded border bg-muted/30 p-3 text-xs">
                  {prettyJson(selectedLog.afterValue)}
                </pre>
              </div>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">메타데이터</p>
              <pre className="mt-1 max-h-64 overflow-auto rounded border bg-muted/30 p-3 text-xs">
                {prettyJson(selectedLog.metadata)}
              </pre>
            </div>
          </div>
        ) : null}
      </AdminCrudModal>
    </AdminPageShell>
  );
}
