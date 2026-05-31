import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { formatSecurityDateTime } from "@/lib/dateTime";
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
  statusTone,
} from "@/pages/admin/adminUi";
import {
  disableAdminUser,
  enableAdminUser,
  getAdminFilterOptions,
  getAdminUsers,
  lockAdminUser,
  unlockAdminUser,
} from "@/services/AdminService";

const display = (value?: string | null) => value || "-";

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
    setFilters({ keyword: "", status: "", role: "" });
    const page = await getAdminUsers({ page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
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
    <AdminPageShell title="사용자 관리" description="계정 상태, 인증 방식, 조직 정보를 조회하고 관리자 조치를 수행합니다.">
      <AdminFilters
        fields={[
          { name: "keyword", label: "사용자 검색", placeholder: "아이디, 이메일, 이름" },
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
                  <AdminSortableHeader label="사용자 ID" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>이름</th>
                <th className={adminCellClassName}>이메일</th>
                <th className={adminCellClassName}>사번</th>
                <th className={adminCellClassName}>부서</th>
                <th className={adminCellClassName}>직급</th>
                <th className={adminCellClassName}>상태</th>
                <th className={adminCellClassName}>유형</th>
                <th className={adminCellClassName}>인증</th>
                <th className={adminCellClassName}>MFA</th>
                <th className={adminCellClassName}>마지막 로그인</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? <AdminEmptyRow colSpan={12} /> : null}
              {users.map((item) => (
                <tr key={item.username} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <Link className="font-medium text-primary hover:underline" to={`/admin/account/users/${item.username}`}>
                      {item.username}
                    </Link>
                    <div className="text-xs text-muted-foreground">{item.roles?.join(", ") || "ROLE_USER"}</div>
                  </td>
                  <td className={adminCellClassName}>{display(item.name ?? item.nickname)}</td>
                  <td className={adminCellClassName}>{display(item.email)}</td>
                  <td className={adminCellClassName}>{display(item.employeeNo)}</td>
                  <td className={adminCellClassName}>{display(item.department)}</td>
                  <td className={adminCellClassName}>{display(item.position)}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(item.status)}>{item.status || "ACTIVE"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{item.userType || (item.social ? "SOCIAL" : "INTERNAL")}</td>
                  <td className={adminCellClassName}>{item.authMethod || "PASSWORD"}</td>
                  <td className={adminCellClassName}>{item.mfaEnabled ? "ON" : "OFF"}</td>
                  <td className={adminCellClassName}>{item.lastLoginAt ? formatSecurityDateTime(item.lastLoginAt) : "-"}</td>
                  <td className={adminCellClassName}>
                    <div className="flex flex-wrap gap-2">
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
                      <Button
                        size="sm"
                        variant={item.enabled ? "destructive" : "outline"}
                        disabled={item.username === "admin"}
                        onClick={async () => {
                          item.enabled ? await disableAdminUser(item.username) : await enableAdminUser(item.username);
                          await load();
                        }}
                      >
                        {item.enabled ? "비활성화" : "활성화"}
                      </Button>
                    </div>
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
