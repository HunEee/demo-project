import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { AdminFilterOptions, AdminUser } from "@/models/AdminModels";
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
} from "@/pages/admin/adminUi";
import { getAdminFilterOptions, getAdminUsers, lockAdminUser, unlockAdminUser } from "@/services/AdminService";

export default function AdminUsersPage() {
  const user = useAuth((state) => state.user);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", role: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "createdAt", direction: "DESC" });
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminUsers({
          keyword: filters.keyword,
          status: filters.status,
          role: filters.role,
          page: nextPage,
          size: pageState.size,
          sort: nextSort.sort,
          direction: nextSort.direction,
        });
    setUsers(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const handleSort = (column: string) => {
    const nextSort: SortState = {
      sort: column,
      direction: sortState.sort === column && sortState.direction === "DESC" ? "ASC" : "DESC",
    };
    setSortState(nextSort);
    void load(0, nextSort);
  };

  const resetFilters = async () => {
    const nextFilters = { keyword: "", status: "", role: "" };
    setFilters(nextFilters);
    const page = await getAdminUsers({
      keyword: nextFilters.keyword,
      status: nextFilters.status,
      role: nextFilters.role,
      page: 0,
      size: pageState.size,
      sort: sortState.sort,
      direction: sortState.direction,
    });
    setUsers(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  useEffect(() => {
    if (isAdmin) {
      void load();
      void getAdminFilterOptions().then(setFilterOptions);
    }
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="사용자 관리"
      description="계정 상태를 조회하고 잠금 처리를 수행합니다."
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "사용자 검색", placeholder: "아이디, 이메일, 닉네임" },
          {
            name: "status",
            label: "계정 상태",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.userStatuses ?? [])],
          },
          {
            name: "role",
            label: "권한",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.roles ?? [])],
          },
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
                  <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="이메일" column="email" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>권한</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="enabled" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? <AdminEmptyRow colSpan={5} /> : null}
              {users.map((item) => (
                <tr key={item.username} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <Link className="font-medium text-primary hover:underline" to={`/admin/users/${item.username}`}>
                      {item.username}
                    </Link>
                    <div className="text-xs text-muted-foreground">{item.nickname || "-"}</div>
                  </td>
                  <td className={adminCellClassName}>{item.email || "-"}</td>
                  <td className={adminCellClassName}>{item.roles?.join(", ") || "ROLE_USER"}</td>
                  <td className={adminCellClassName}>
                    {item.deleted ? (
                      <AdminBadge tone="danger">탈퇴</AdminBadge>
                    ) : item.locked ? (
                      <AdminBadge tone="danger">잠금</AdminBadge>
                    ) : item.enabled ? (
                      <AdminBadge tone="success">활성</AdminBadge>
                    ) : (
                      <AdminBadge>비활성</AdminBadge>
                    )}
                  </td>
                  <td className={adminCellClassName}>
                    <Button
                      size="sm"
                      variant={item.locked ? "outline" : "destructive"}
                      disabled={item.username === "admin"}
                      onClick={async () => {
                        item.locked ? await unlockAdminUser(item.username) : await lockAdminUser(item.username);
                        await load();
                      }}
                    >
                      {item.locked ? "잠금 해제" : "잠금"}
                    </Button>
                  </td>
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
