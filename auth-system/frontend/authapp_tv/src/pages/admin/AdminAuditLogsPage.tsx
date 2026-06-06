import { useCallback, useEffect, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { formatSecurityDateTime } from "@/lib/dateTime";
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
} from "@/pages/admin/adminUi";
import { useAdminServerList } from "@/pages/admin/useAdminList";
import type { AdminAuditLog, AdminFilterOptions } from "@/models/AdminModels";
import { getAdminAuditLogs, getAdminFilterOptions } from "@/services/AdminService";

const initialFilters = { username: "", type: "", from: "", to: "" };

export default function AdminAuditLogsPage() {
  const user = useAuth((state) => state.user);
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const fetchPage = useCallback((params: typeof initialFilters & { page: number; size: number; sort: string; direction: "ASC" | "DESC" }) => getAdminAuditLogs(params), []);
  const { items, filters, pageState, sortState, load, handleFilterChange, handleSort, resetFilters } = useAdminServerList<AdminAuditLog, typeof initialFilters>({
    initialFilters,
    initialSort: { sort: "createdAt", direction: "DESC" },
    fetchPage,
  });

  useEffect(() => {
    if (isAdmin) {
      void load().catch(() => undefined);
      void getAdminFilterOptions().then(setFilterOptions).catch(() => undefined);
    }
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell title="감사 로그" description="계정과 인증 흐름에서 발생한 운영 이벤트입니다.">
      <AdminFilters
        fields={[
          { name: "username", label: "사용자 검색", placeholder: "아이디" },
          { name: "type", label: "이벤트 유형", type: "select", options: [{ label: "전체", value: "" }, ...(filterOptions?.auditEventTypes ?? [])] },
          { name: "from", label: "시작일", type: "date" },
          { name: "to", label: "종료일", type: "date" },
        ]}
        values={filters}
        onChange={handleFilterChange}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <AdminTableCard>
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="시간" column="createdAt" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="유형" column="type" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>설명</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="IP" column="ipAddress" sortState={sortState} onSort={handleSort} />
                </th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? <AdminEmptyRow colSpan={5} /> : null}
              {items.map((item) => (
                <tr key={item.id} className={adminRowClassName}>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(item.createdAt)}
                  </td>
                  <td className={adminCellClassName}>{item.username}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone="info">{item.type}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{item.description || "-"}</td>
                  <td className={adminCellClassName}>{item.ipAddress || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
      </AdminTableCard>
    </AdminPageShell>
  );
}
