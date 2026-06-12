import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import { hasAdminAccess } from "@/auth/permissions";
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
  type PageState,
  type SortState,
  adminCellClassName,
  adminRowClassName,
  adminTableClassName,
  adminTheadClassName,
} from "@/pages/admin/adminUi";
import type { AdminAuditLog, AdminFilterOptions } from "@/models/AdminModels";
import { getAdminAuditLogs, getAdminFilterOptions } from "@/services/AdminService";

const initialFilters = { username: "", type: "", from: "", to: "" };

export default function AdminAuditLogsPage() {
  const user = useAuth((state) => state.user);
  const [items, setItems] = useState<AdminAuditLog[]>([]);
  const [filters, setFilters] = useState(initialFilters);
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "createdAt", direction: "DESC" });
  const isAdmin = hasAdminAccess(user);

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminAuditLogs({
      username: filters.username,
      type: filters.type,
      from: filters.from,
      to: filters.to,
      page: nextPage,
      size: pageState.size,
      sort: nextSort.sort,
      direction: nextSort.direction,
    });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const handleFilterChange = (name: string, value: string) => {
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleSort = (column: string) => {
    const nextSort: SortState = {
      sort: column,
      direction: sortState.sort === column && sortState.direction === "DESC" ? "ASC" : "DESC",
    };
    setSortState(nextSort);
    void load(0, nextSort).catch(() => undefined);
  };

  const resetFilters = async () => {
    setFilters(initialFilters);
    const page = await getAdminAuditLogs({
      username: initialFilters.username,
      type: initialFilters.type,
      from: initialFilters.from,
      to: initialFilters.to,
      page: 0,
      size: pageState.size,
      sort: sortState.sort,
      direction: sortState.direction,
    });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

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
