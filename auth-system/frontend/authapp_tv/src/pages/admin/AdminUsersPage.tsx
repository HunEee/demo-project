import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminFilterOptions, AdminUser } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminConfirmDialog,
  AdminCrudModal,
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
  createAdminUser,
  deleteAdminUser,
  disableAdminUser,
  enableAdminUser,
  getAdminFilterOptions,
  getAdminUsers,
  lockAdminUser,
  unlockAdminUser,
  updateAdminUser,
} from "@/services/AdminService";

const display = (value?: string | null) => value || "-";
const blankUserForm = {
  username: "",
  password: "",
  email: "",
  name: "",
  employeeNo: "",
  departmentId: "",
  position: "",
  employmentType: "EMPLOYEE",
  status: "ACTIVE",
  expiresAt: "",
  roleName: "ROLE_USER",
  reason: "",
};
const accountStatus = (item: AdminUser) => {
  if (item.deleted) return "DELETED";
  return item.status || "ACTIVE";
};

export default function AdminUsersPage() {
  const user = useAuth((state) => state.user);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", role: "", employmentType: "", mfaEnabled: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "createdAt", direction: "DESC" });
  const [drawerMode, setDrawerMode] = useState<"create" | "edit" | null>(null);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [userForm, setUserForm] = useState(blankUserForm);
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminUsers({
      keyword: filters.keyword,
      status: filters.status,
      role: filters.role,
      employmentType: filters.employmentType,
      mfaEnabled: filters.mfaEnabled,
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
    void load(0, nextSort).catch(() => undefined);
  };

  const resetFilters = async () => {
    setFilters({ keyword: "", status: "", role: "", employmentType: "", mfaEnabled: "" });
    const page = await getAdminUsers({ page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
    setUsers(page.content);
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
    <AdminPageShell
      title="사용자 관리"
      description="계정 상태, 인증 방식, 조직 정보를 조회하고 관리자 조치를 수행합니다."
      actions={
        <Button
          type="button"
          onClick={() => {
            setSelectedUser(null);
            setUserForm(blankUserForm);
            setDrawerMode("create");
          }}
        >
          사용자 추가
        </Button>
      }
    >
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
          {
            name: "employmentType",
            label: "고용 형태",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.employmentTypes ?? [])],
          },
          {
            name: "mfaEnabled",
            label: "MFA",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "사용", value: "true" },
              { label: "미사용", value: "false" },
            ],
          },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0).catch(() => undefined)}
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
                    <AdminBadge tone={statusTone(accountStatus(item))}>{accountStatus(item)}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{item.userType || (item.social ? "SOCIAL" : "INTERNAL")}</td>
                  <td className={adminCellClassName}>{item.authMethod || "PASSWORD"}</td>
                  <td className={adminCellClassName}>{item.mfaEnabled ? "ON" : "OFF"}</td>
                  <td className={adminCellClassName}>{item.lastLoginAt ? formatSecurityDateTime(item.lastLoginAt) : "-"}</td>
                  <td className={adminCellClassName}>
                    <div className="flex flex-wrap gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setSelectedUser(item);
                          setUserForm({
                            username: item.username,
                            password: "",
                            email: item.email || "",
                            name: item.name || item.nickname || "",
                            employeeNo: item.employeeNo || "",
                            departmentId: item.departmentId ? String(item.departmentId) : "",
                            position: item.position || "",
                            employmentType: item.employmentType || "EMPLOYEE",
                            status: item.status || "ACTIVE",
                            expiresAt: item.expiresAt ? item.expiresAt.slice(0, 10) : "",
                            roleName: item.roles?.[0] || "ROLE_USER",
                            reason: "",
                          });
                          setDrawerMode("edit");
                        }}
                      >
                        수정
                      </Button>
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
                      <Button
                        size="sm"
                        variant="destructive"
                        disabled={item.username === "admin" || item.deleted}
                        onClick={() => setDeleteTarget(item)}
                      >
                        삭제
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

      <AdminCrudModal
        open={drawerMode !== null}
        title={drawerMode === "create" ? "사용자 추가" : "사용자 수정"}
        description="기본 계정 정보와 조직 프로필을 입력합니다."
        onOpenChange={(open) => {
          if (!open) setDrawerMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setDrawerMode(null)}>
              취소
            </Button>
            <Button
              type="button"
              onClick={async () => {
                const payload = {
                  email: userForm.email,
                  name: userForm.name,
                  employeeNo: userForm.employeeNo,
                  departmentId: userForm.departmentId ? Number(userForm.departmentId) : null,
                  position: userForm.position,
                  employmentType: userForm.employmentType,
                  status: userForm.status,
                  expiresAt: userForm.expiresAt,
                  reason: userForm.reason,
                };
                if (drawerMode === "create") {
                  await createAdminUser({ ...payload, username: userForm.username, password: userForm.password, roleName: userForm.roleName });
                } else if (selectedUser) {
                  await updateAdminUser(selectedUser.username, payload);
                }
                setDrawerMode(null);
                await load(0);
              }}
            >
              저장
            </Button>
          </>
        }
      >
        {drawerMode === "create" ? <Field label="사용자 ID" value={userForm.username} onChange={(value) => setUserForm((prev) => ({ ...prev, username: value }))} /> : null}
        {drawerMode === "create" ? <Field label="초기 비밀번호" type="password" value={userForm.password} onChange={(value) => setUserForm((prev) => ({ ...prev, password: value }))} /> : null}
        <Field label="이름" value={userForm.name} onChange={(value) => setUserForm((prev) => ({ ...prev, name: value }))} />
        <Field label="이메일" type="email" value={userForm.email} onChange={(value) => setUserForm((prev) => ({ ...prev, email: value }))} />
        <Field label="사번" value={userForm.employeeNo} onChange={(value) => setUserForm((prev) => ({ ...prev, employeeNo: value }))} />
        <Field label="부서 ID" type="number" value={userForm.departmentId} onChange={(value) => setUserForm((prev) => ({ ...prev, departmentId: value }))} />
        <Field label="직급" value={userForm.position} onChange={(value) => setUserForm((prev) => ({ ...prev, position: value }))} />
        <Field label="고용 형태" value={userForm.employmentType} onChange={(value) => setUserForm((prev) => ({ ...prev, employmentType: value }))} />
        <Field label="상태" value={userForm.status} onChange={(value) => setUserForm((prev) => ({ ...prev, status: value }))} />
        <Field label="만료일" type="date" value={userForm.expiresAt} onChange={(value) => setUserForm((prev) => ({ ...prev, expiresAt: value }))} />
        {drawerMode === "create" ? <Field label="초기 역할" value={userForm.roleName} onChange={(value) => setUserForm((prev) => ({ ...prev, roleName: value }))} /> : null}
        <Field label="사유" value={userForm.reason} onChange={(value) => setUserForm((prev) => ({ ...prev, reason: value }))} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={deleteTarget !== null}
        title="사용자 삭제"
        description={`${deleteTarget?.username ?? ""} 계정을 삭제 처리합니다. 삭제된 계정은 로그인할 수 없습니다.`}
        confirmLabel="삭제"
        destructive
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        onConfirm={() => {
          if (!deleteTarget) return;
          void deleteAdminUser(deleteTarget.username, "관리자 사용자 삭제").then(() => load());
        }}
      />
    </AdminPageShell>
  );
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <Input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}
