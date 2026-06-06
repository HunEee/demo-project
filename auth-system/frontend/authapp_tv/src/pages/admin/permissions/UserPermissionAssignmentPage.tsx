import { useEffect, useMemo, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import type { AdminRole, AdminUser } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminBulkActionBar,
  AdminConfirmDialog,
  AdminCrudModal,
  AdminEmptyRow,
  AdminFormField,
  AdminPagination,
  AdminSelectField,
  AdminSortableHeader,
  AdminTableCard,
  type PageState,
  type SortState,
  adminCellClassName,
  adminRowClassName,
  adminTableClassName,
  adminTheadClassName,
  displayValue as display,
  statusTone,
} from "@/pages/admin/adminUi";
import {
  assignAdminUserRole,
  getAdminRoles,
  getAdminUsers,
  removeAdminUserRole,
} from "@/services/AdminService";

const blankAssignment = {
  roleName: "",
  reason: "",
  sensitiveReason: "",
};

const accountStatusLabel = (status?: string | null) => {
  switch (status) {
    case "LOCKED":
      return "잠금";
    case "DISABLED":
      return "비활성";
    case "DELETED":
      return "삭제";
    case "ACTIVE":
    default:
      return "활성";
  }
};

export default function UserPermissionAssignmentPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", role: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "username", direction: "ASC" });
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [assignmentForm, setAssignmentForm] = useState(blankAssignment);
  const [selectedRevokes, setSelectedRevokes] = useState<Array<{ username: string; roleId: number; roleName: string }>>([]);
  const [bulkRevokeOpen, setBulkRevokeOpen] = useState(false);

  const roleOptions = useMemo(
    () => [{ label: "역할 선택", value: "" }, ...roles.filter((role) => role.enabled).map((role) => ({ label: `${role.name} ${role.displayName ? `(${role.displayName})` : ""}`, value: role.name }))],
    [roles],
  );
  const selectedRole = roles.find((role) => role.name === assignmentForm.roleName);

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const [roleList, page] = await Promise.all([
      getAdminRoles(),
      getAdminUsers({
        keyword: filters.keyword,
        status: filters.status,
        role: filters.role,
        page: nextPage,
        size: pageState.size,
        sort: nextSort.sort,
        direction: nextSort.direction,
      }),
    ]);
    setRoles(roleList);
    setUsers(page.content);
    setSelectedRevokes([]);
    setPageState((current) => ({
      ...current,
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
    }));
  };

  useEffect(() => {
    if (isAdmin) void load(0).catch(() => undefined);
  }, [isAdmin]);

  const handleSort = (column: string) => {
    const nextSort: SortState = {
      sort: column,
      direction: sortState.sort === column && sortState.direction === "ASC" ? "DESC" : "ASC",
    };
    setSortState(nextSort);
    void load(0, nextSort).catch(() => undefined);
  };

  const resetFilters = async () => {
    setFilters({ keyword: "", status: "", role: "" });
    const page = await getAdminUsers({ page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
    setUsers(page.content);
    setSelectedRevokes([]);
    setPageState((current) => ({ ...current, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const openAssignmentModal = (target: AdminUser) => {
    setSelectedUser(target);
    setAssignmentForm(blankAssignment);
  };

  const saveAssignment = async () => {
    if (!selectedUser || !assignmentForm.roleName) return;
    await assignAdminUserRole(selectedUser.username, {
      roleName: assignmentForm.roleName,
      reason: assignmentForm.reason || "사용자 역할 부여",
      sensitiveReason: selectedRole?.sensitive ? assignmentForm.sensitiveReason : undefined,
    });
    setSelectedUser(null);
    setAssignmentForm(blankAssignment);
    await load(pageState.page);
  };

  const revokeKey = (username: string, roleId: number) => `${username}:${roleId}`;
  const isRevokeSelected = (username: string, roleId: number) =>
    selectedRevokes.some((item) => item.username === username && item.roleId === roleId);

  const runBulkRevoke = async () => {
    if (selectedRevokes.length === 0) return;
    await Promise.all(selectedRevokes.map((target) => removeAdminUserRole(target.username, target.roleId, "사용자 역할 선택 회수")));
    setSelectedRevokes([]);
    setBulkRevokeOpen(false);
    await load(pageState.page);
  };

  const getUserRevokes = (target: AdminUser) =>
    (target.roles?.length ? target.roles : ["ROLE_USER"])
      .map((roleName) => roles.find((candidate) => candidate.name === roleName))
      .filter((role): role is AdminRole => Boolean(role))
      .map((role) => ({ username: target.username, roleId: role.id, roleName: role.name }));
  const selectableUsers = users.filter((item) => getUserRevokes(item).length > 0);
  const selectedUsernames = new Set(selectedRevokes.map((item) => item.username));
  const allPageSelected =
    selectableUsers.length > 0 && selectableUsers.every((item) => getUserRevokes(item).every((target) => isRevokeSelected(target.username, target.roleId)));

  const toggleUserRevokes = (target: AdminUser, checked: boolean) => {
    const targets = getUserRevokes(target);
    setSelectedRevokes((current) => {
      const targetKeys = new Set(targets.map((item) => revokeKey(item.username, item.roleId)));
      const next = current.filter((item) => !targetKeys.has(revokeKey(item.username, item.roleId)));
      return checked ? [...next, ...targets] : next;
    });
  };

  const togglePageRevokes = (checked: boolean) => {
    const targets = selectableUsers.flatMap((item) => getUserRevokes(item));
    setSelectedRevokes((current) => {
      const targetKeys = new Set(targets.map((item) => revokeKey(item.username, item.roleId)));
      const next = current.filter((item) => !targetKeys.has(revokeKey(item.username, item.roleId)));
      return checked ? [...next, ...targets] : next;
    });
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="사용자 권한 할당"
      description="사용자별 역할을 조회하고 필요한 역할을 부여하거나 회수합니다."
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "사용자 검색", placeholder: "아이디, 이메일, 이름" },
          {
            name: "status",
            label: "계정 상태",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "활성", value: "ACTIVE" },
              { label: "잠금", value: "LOCKED" },
              { label: "비활성", value: "DISABLED" },
            ],
          },
          { name: "role", label: "역할", type: "select", options: [{ label: "전체", value: "" }, ...roles.map((role) => ({ label: role.name, value: role.name }))] },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((current) => ({ ...current, [name]: value }))}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <AdminBulkActionBar selectedLabel={`선택 사용자 ${selectedUsernames.size}명 / 회수 역할 ${selectedRevokes.length}건`}>
        <Button type="button" variant="destructive" disabled={selectedRevokes.length === 0} onClick={() => setBulkRevokeOpen(true)}>
          선택 회수
        </Button>
      </AdminBulkActionBar>

      <AdminTableCard>
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <input
                    type="checkbox"
                    aria-label="현재 페이지 사용자 역할 전체 선택"
                    checked={allPageSelected}
                    onChange={(event) => togglePageRevokes(event.target.checked)}
                  />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자 ID" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>이름</th>
                <th className={adminCellClassName}>이메일</th>
                <th className={adminCellClassName}>부서</th>
                <th className={adminCellClassName}>상태</th>
                <th className={adminCellClassName}>현재 역할</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? <AdminEmptyRow colSpan={8} /> : null}
              {users.map((item) => (
                <tr key={item.username} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <input
                      type="checkbox"
                      aria-label={`${item.username} 사용자 역할 선택`}
                      disabled={getUserRevokes(item).length === 0}
                      checked={getUserRevokes(item).length > 0 && getUserRevokes(item).every((target) => isRevokeSelected(target.username, target.roleId))}
                      onChange={(event) => toggleUserRevokes(item, event.target.checked)}
                    />
                  </td>
                  <td className={adminCellClassName}>
                    <Link className="font-medium text-primary hover:underline" to={`/admin/account/users/${item.username}`}>
                      {item.username}
                    </Link>
                  </td>
                  <td className={adminCellClassName}>{display(item.name ?? item.nickname)}</td>
                  <td className={adminCellClassName}>{display(item.email)}</td>
                  <td className={adminCellClassName}>{display(item.department)}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(item.status)}>{accountStatusLabel(item.status)}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <div className="flex flex-wrap justify-center gap-2">
                      {(item.roles?.length ? item.roles : ["ROLE_USER"]).map((roleName) => {
                        const role = roles.find((candidate) => candidate.name === roleName);
                        return (
                          <span key={roleName} className="inline-flex items-center gap-1">
                            <AdminBadge tone={role?.sensitive ? "warning" : "default"}>{roleName}</AdminBadge>
                          </span>
                        );
                      })}
                    </div>
                  </td>
                  <td className={adminCellClassName}>
                    <Button size="sm" variant="outline" onClick={() => openAssignmentModal(item)}>역할 부여</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
      </AdminTableCard>

      <AdminCrudModal
        open={selectedUser !== null}
        title={selectedUser ? `${selectedUser.username} 역할 부여` : "역할 부여"}
        description="민감 권한이 포함된 역할은 별도 부여 사유를 남깁니다."
        onOpenChange={(open) => {
          if (!open) setSelectedUser(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setSelectedUser(null)}>취소</Button>
            <Button type="button" onClick={() => void saveAssignment()}>부여</Button>
          </>
        }
      >
        <AdminSelectField
          label="역할"
          value={assignmentForm.roleName}
          options={roleOptions}
          onChange={(value) => setAssignmentForm((current) => ({ ...current, roleName: value }))}
        />
        <AdminFormField label="부여 사유" value={assignmentForm.reason} onChange={(value) => setAssignmentForm((current) => ({ ...current, reason: value }))} />
        {selectedRole?.sensitive ? (
          <AdminFormField
            label="민감 권한 부여 사유"
            value={assignmentForm.sensitiveReason}
            onChange={(value) => setAssignmentForm((current) => ({ ...current, sensitiveReason: value }))}
          />
        ) : null}
      </AdminCrudModal>

      <AdminConfirmDialog
        open={bulkRevokeOpen}
        title="사용자 역할 선택 회수"
        description={`${selectedRevokes.length}건의 사용자 역할을 회수합니다.`}
        confirmLabel="회수"
        destructive
        onOpenChange={setBulkRevokeOpen}
        onConfirm={() => {
          void runBulkRevoke().catch(() => undefined);
        }}
      />
    </AdminPageShell>
  );
}
