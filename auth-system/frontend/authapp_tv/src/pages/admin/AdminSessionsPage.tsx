import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import { Ban } from "lucide-react";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
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
import type { AdminFilterOptions, AdminSession } from "@/models/AdminModels";
import { getAdminFilterOptions, getAdminSessions, revokeAdminSession } from "@/services/AdminService";

export default function AdminSessionsPage() {
  const user = useAuth((state) => state.user);
  const [items, setItems] = useState<AdminSession[]>([]);
  const [filters, setFilters] = useState({ username: "", status: "", from: "", to: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "lastUsedAt", direction: "DESC" });
  const isAdmin = hasAdminAccess(user);
  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminSessions({
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
    const page = await getAdminSessions({ username: nextFilters.username, status: nextFilters.status, from: nextFilters.from, to: nextFilters.to, page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
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
    <AdminPageShell title="세션/토큰 관리" description="refresh token 기반 세션을 조회하고 즉시 폐기합니다.">
      <AdminFilters
        fields={[
          { name: "username", label: "사용자 검색", placeholder: "아이디" },
          {
            name: "status",
            label: "세션 상태",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.sessionStatuses ?? [])],
          },
          { name: "from", label: "최근 사용 시작일", type: "date" },
          { name: "to", label: "최근 사용 종료일", type: "date" },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <AdminTableCard>
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>디바이스</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="IP" column="ipAddress" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="최근 사용" column="lastUsedAt" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="만료" column="expiresAt" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="revoked" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
              {items.map((item) => (
                <tr key={item.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>{item.username}</td>
                  <td className={adminCellClassName}>{item.device || "-"}</td>
                  <td className={adminCellClassName}>{item.ipAddress || "-"}</td>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(item.lastUsedAt)}
                  </td>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(item.expiresAt)}
                  </td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={item.revoked ? "danger" : "success"}>{item.revoked ? "폐기" : "활성"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <Button
                      size="sm"
                      variant="destructive"
                      disabled={item.revoked}
                      onClick={async () => {
                        await revokeAdminSession(item.id);
                        await load();
                      }}
                    >
                      <Ban className="h-4 w-4" />
                      폐기
                    </Button>
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
