import { useEffect, useMemo, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AdminRole, AdminUser } from "@/models/AdminModels";
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

const display = (value?: string | number | null) => (value === null || value === undefined || value === "" ? "-" : String(value));
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
  const [revokeTarget, setRevokeTarget] = useState<{ username: string; role: AdminRole } | null>(null);

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
                <th className={adminCellClassName}>부서</th>
                <th className={adminCellClassName}>상태</th>
                <th className={adminCellClassName}>현재 역할</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
              {users.map((item) => (
                <tr key={item.username} className={adminRowClassName}>
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
                            {role ? (
                              <Button size="xs" variant="ghost" onClick={() => setRevokeTarget({ username: item.username, role })}>
                                회수
                              </Button>
                            ) : null}
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
        </CardContent>
      </Card>

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
        <SelectField
          label="역할"
          value={assignmentForm.roleName}
          options={roleOptions}
          onChange={(value) => setAssignmentForm((current) => ({ ...current, roleName: value }))}
        />
        <Field label="부여 사유" value={assignmentForm.reason} onChange={(value) => setAssignmentForm((current) => ({ ...current, reason: value }))} />
        {selectedRole?.sensitive ? (
          <Field
            label="민감 권한 부여 사유"
            value={assignmentForm.sensitiveReason}
            onChange={(value) => setAssignmentForm((current) => ({ ...current, sensitiveReason: value }))}
          />
        ) : null}
      </AdminCrudModal>

      <AdminConfirmDialog
        open={revokeTarget !== null}
        title="사용자 역할 회수"
        description={`${revokeTarget?.username ?? ""} 사용자에게서 ${revokeTarget?.role.name ?? ""} 역할을 회수합니다.`}
        confirmLabel="회수"
        destructive
        onOpenChange={(open) => {
          if (!open) setRevokeTarget(null);
        }}
        onConfirm={() => {
          if (!revokeTarget) return;
          void removeAdminUserRole(revokeTarget.username, revokeTarget.role.id, "사용자 역할 회수").then(() => load(pageState.page));
        }}
      />
    </AdminPageShell>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <Input value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Array<{ label: string; value: string }>;
  onChange: (value: string) => void;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <select
        className="h-9 w-full rounded-lg border border-input bg-background px-2.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}
