import { useCallback, useEffect, useState } from "react";
import { Eye } from "lucide-react";
import { Navigate } from "react-router";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import type { AdminActionLog } from "@/models/AdminModels";
import AdminDateTimeCell from "@/pages/admin/AdminDateTimeCell";
import AdminFilters from "@/pages/admin/AdminFilters";
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
} from "@/pages/admin/adminUi";
import { useAdminServerList } from "@/pages/admin/useAdminList";
import { getAdminActionLogs } from "@/services/AdminService";

const initialFilters = {
  actor: "",
  target: "",
  action: "",
  from: "",
  to: "",
};

const policyActionOptions = [
  { label: "전체", value: "" },
  { label: "정책 변경", value: "UPDATE_POLICY" },
  { label: "MFA 정책 변경", value: "MFA_POLICY_UPDATED" },
  { label: "정책 변경", value: "SECURITY_POLICY_UPDATED" },
];

const policyActionLabel = (value?: string) =>
  policyActionOptions.find((option) => option.value === value)?.label ?? displayValue(value);

function prettyJson(value?: string) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

export default function PolicyChangeHistoryPage() {
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

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin, load]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="정책 변경 이력"
      description="인증, MFA, 보안 정책 변경 작업을 관리자 감사 로그 기준으로 조회합니다."
    >
      <AdminFilters
        fields={[
          { name: "actor", label: "수행자", placeholder: "관리자 ID" },
          { name: "target", label: "정책 대상", placeholder: "POLICY, MFA_POLICY" },
          { name: "action", label: "작업", type: "select", options: policyActionOptions },
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
              <th className={adminCellClassName}>정책 대상</th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="작업" column="actionType" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>사유</th>
              <th className={adminCellClassName}>상세</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? <AdminEmptyRow colSpan={6} /> : null}
            {items.map((item) => (
              <tr key={item.id} className={adminRowClassName}>
                <td className={adminCellClassName}>
                  <AdminDateTimeCell value={item.createdAt} />
                </td>
                <td className={adminCellClassName}>{displayValue(item.actorUsername)}</td>
                <td className={adminCellClassName}>{displayValue(item.targetType)}</td>
                <td className={adminCellClassName}>
                  <AdminBadge tone="info">{policyActionLabel(item.actionType)}</AdminBadge>
                </td>
                <td className={`${adminCellClassName} max-w-56`}>{displayValue(item.reason)}</td>
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
        title="정책 변경 상세"
        description="정책 변경 작업의 변경 전/후 데이터와 사유를 확인합니다."
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
                <p className="mt-1 font-medium">{policyActionLabel(selectedLog.actionType)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">정책 대상</p>
                <p className="mt-1 font-medium">{displayValue(selectedLog.targetType)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">사유</p>
                <p className="mt-1 font-medium">{displayValue(selectedLog.reason)}</p>
              </div>
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
          </div>
        ) : null}
      </AdminCrudModal>
    </AdminPageShell>
  );
}
