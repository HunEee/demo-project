import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Card, CardContent } from "@/components/ui/card";
import { formatSecurityDateTime } from "@/lib/dateTime";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminEmptyRow,
  AdminPagination,
  AdminSortableHeader,
  type PageState,
  type SortState,
  adminCellClassName,
  adminRowClassName,
  adminTableClassName,
  adminTheadClassName,
  statusTone,
} from "@/pages/admin/adminUi";
import type { AdminFilterOptions, AdminLoginHistory } from "@/models/AdminModels";
import { getAdminFilterOptions, getAdminLoginHistory } from "@/services/AdminService";

export default function AdminLoginHistoryPage() {
  const user = useAuth((state) => state.user);
  const [items, setItems] = useState<AdminLoginHistory[]>([]);
  const [filters, setFilters] = useState({ username: "", status: "", from: "", to: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "loginAt", direction: "DESC" });
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminLoginHistory({
          username: filters.username,
          status: filters.status,
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

  const handleSort = (column: string) => {
    const nextSort: SortState = {
      sort: column,
      direction: sortState.sort === column && sortState.direction === "DESC" ? "ASC" : "DESC",
    };
    setSortState(nextSort);
    void load(0, nextSort).catch(() => undefined);
  };

  const resetFilters = async () => {
    const nextFilters = { username: "", status: "", from: "", to: "" };
    setFilters(nextFilters);
    const page = await getAdminLoginHistory({ username: nextFilters.username, status: nextFilters.status, from: nextFilters.from, to: nextFilters.to, page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
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
    <AdminPageShell title="전체 로그인 이력" description="모든 사용자의 로그인 성공과 실패 기록입니다.">
      <AdminFilters
        fields={[
          { name: "username", label: "사용자 검색", placeholder: "아이디" },
          {
            name: "status",
            label: "로그인 상태",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.loginStatuses ?? [])],
          },
          { name: "from", label: "시작일", type: "date" },
          { name: "to", label: "종료일", type: "date" },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="시간" column="loginAt" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="status" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="IP" column="ipAddress" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="디바이스" column="device" sortState={sortState} onSort={handleSort} />
                </th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? <AdminEmptyRow colSpan={5} /> : null}
              {items.map((item) => (
                <tr key={item.id} className={adminRowClassName}>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(item.loginAt)}
                  </td>
                  <td className={adminCellClassName}>{item.username}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(item.status)}>{item.status}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{item.ipAddress || "-"}</td>
                  <td className={adminCellClassName}>{item.device || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
        </CardContent>
      </Card>
    </AdminPageShell>
  );
}
