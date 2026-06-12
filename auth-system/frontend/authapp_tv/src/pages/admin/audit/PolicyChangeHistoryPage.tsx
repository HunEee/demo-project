import { useCallback, useEffect } from "react";
import { Navigate } from "react-router";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
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
];

const policyActionLabel = (value?: string) =>
  policyActionOptions.find((option) => option.value === value)?.label ?? displayValue(value);

export default function PolicyChangeHistoryPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = hasAdminAccess(user);

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
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell title="정책 변경 이력" description="인증, MFA, 보안 정책 변경 작업을 관리자 감사 로그 기준으로 조회합니다.">
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
        <table className={`${adminTableClassName} min-w-[960px]`}>
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
              <th className={adminCellClassName}>변경 전</th>
              <th className={adminCellClassName}>변경 후</th>
              <th className={adminCellClassName}>사유</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
            {items.map((item) => (
              <tr key={item.id} className={adminRowClassName}>
                <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>{formatSecurityDateTime(item.createdAt)}</td>
                <td className={adminCellClassName}>{displayValue(item.actorUsername)}</td>
                <td className={adminCellClassName}>{displayValue(item.targetType)}</td>
                <td className={adminCellClassName}>
                  <AdminBadge tone="info">{policyActionLabel(item.actionType)}</AdminBadge>
                </td>
                <td className={`${adminCellClassName} max-w-64 text-left font-mono text-xs`}>{displayValue(item.beforeValue)}</td>
                <td className={`${adminCellClassName} max-w-64 text-left font-mono text-xs`}>{displayValue(item.afterValue)}</td>
                <td className={`${adminCellClassName} max-w-56 text-left`}>{displayValue(item.reason)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
      </AdminTableCard>
    </AdminPageShell>
  );
}
