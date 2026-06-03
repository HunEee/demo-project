import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminPermission, AdminRole, AdminRoleDetail, AdminRoleRequest } from "@/models/AdminModels";
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
  assignAdminRolePermission,
  createAdminRole,
  disableAdminRole,
  getAdminPermissions,
  getAdminRoleDetail,
  getAdminRoles,
  removeAdminRolePermission,
  updateAdminRole,
} from "@/services/AdminService";

const blankRoleForm = {
  name: "",
  displayName: "",
  description: "",
  enabled: "true",
  systemRole: "false",
  reason: "",
};

const blankPermissionForm = {
  permissionId: "",
  reason: "",
};

const contains = (value: string | number | boolean | null | undefined, keyword: string) =>
  String(value ?? "").toLowerCase().includes(keyword.trim().toLowerCase());
const compareText = (left?: string | null, right?: string | null) => String(left ?? "").localeCompare(String(right ?? ""));
const display = (value?: string | number | null) => (value === null || value === undefined || value === "" ? "-" : String(value));
const statusLabel = (enabled: boolean) => (enabled ? "사용" : "비활성");

export default function RoleManagementPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [permissions, setPermissions] = useState<AdminPermission[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", sensitive: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "name", direction: "ASC" });
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [roleForm, setRoleForm] = useState(blankRoleForm);
  const [editRole, setEditRole] = useState<AdminRole | null>(null);
  const [disableTarget, setDisableTarget] = useState<AdminRole | null>(null);
  const [detail, setDetail] = useState<AdminRoleDetail | null>(null);
  const [permissionForm, setPermissionForm] = useState(blankPermissionForm);

  const load = async () => {
    const [nextRoles, nextPermissions] = await Promise.all([getAdminRoles(), getAdminPermissions()]);
    setRoles(nextRoles);
    setPermissions(nextPermissions);
    setEditRole((current) => (current ? nextRoles.find((role) => role.id === current.id) ?? current : current));
    if (detail) {
      const stillExists = nextRoles.some((role) => role.id === detail.role.id);
      if (stillExists) setDetail(await getAdminRoleDetail(detail.role.id));
    }
  };

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin]);

  const filteredRoles = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = roles.filter((role) => {
      const status = role.enabled ? "ACTIVE" : "DISABLED";
      return (!keyword || [role.name, role.displayName, role.description].some((value) => contains(value, keyword)))
        && (!filters.status || status === filters.status)
        && (!filters.sensitive || String(role.sensitive) === filters.sensitive);
    });

    return [...filtered].sort((left, right) => {
      const direction = sortState.direction === "ASC" ? 1 : -1;
      const result = (() => {
        switch (sortState.sort) {
          case "displayName":
            return compareText(left.displayName, right.displayName);
          case "permissionCount":
            return left.permissionCount - right.permissionCount;
          case "enabled":
            return Number(left.enabled) - Number(right.enabled);
          case "sensitive":
            return Number(left.sensitive) - Number(right.sensitive);
          case "name":
          default:
            return compareText(left.name, right.name);
        }
      })();
      return result * direction;
    });
  }, [roles, filters, sortState]);

  const listPageState = {
    ...pageState,
    totalElements: filteredRoles.length,
    totalPages: Math.max(Math.ceil(filteredRoles.length / pageState.size), 1),
  };
  const pagedRoles = filteredRoles.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size);
  const assignedPermissionIds = new Set(detail?.permissions.map((permission) => permission.id) ?? []);
  const assignablePermissions = permissions.filter((permission) => permission.enabled && !assignedPermissionIds.has(permission.id));

  const handleSort = (column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
    setPageState((current) => ({ ...current, page: 0 }));
  };

  const openRoleModal = (mode: "create" | "edit", role?: AdminRole) => {
    setModalMode(mode);
    setEditRole(role ?? null);
    setRoleForm(
      role
        ? {
            name: role.name,
            displayName: role.displayName || "",
            description: role.description || "",
            enabled: String(role.enabled),
            systemRole: String(role.systemRole),
            reason: "",
          }
        : blankRoleForm,
    );
  };

  const openRoleDetail = async (role: AdminRole) => {
    setDetail(await getAdminRoleDetail(role.id));
    setPermissionForm(blankPermissionForm);
  };

  const saveRole = async () => {
    const payload: AdminRoleRequest = {
      name: roleForm.name.trim(),
      displayName: roleForm.displayName,
      description: roleForm.description,
      enabled: roleForm.enabled === "true",
      systemRole: roleForm.systemRole === "true",
      reason: roleForm.reason || (modalMode === "create" ? "역할 생성" : "역할 수정"),
    };

    if (modalMode === "edit" && editRole) {
      await updateAdminRole(editRole.id, payload);
    } else {
      await createAdminRole(payload);
    }
    setModalMode(null);
    setRoleForm(blankRoleForm);
    await load();
  };

  const assignPermission = async () => {
    if (!detail || !permissionForm.permissionId) return;
    setDetail(await assignAdminRolePermission(detail.role.id, {
      permissionId: Number(permissionForm.permissionId),
      reason: permissionForm.reason || "역할 권한 추가",
    }));
    setPermissionForm(blankPermissionForm);
    await load();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="역할 관리"
      description="관리자 메뉴 접근을 제어하는 역할을 생성하고 권한을 매핑합니다."
      actions={<Button type="button" onClick={() => openRoleModal("create")}>역할 추가</Button>}
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "역할 검색", placeholder: "역할명, 표시명, 설명" },
          {
            name: "status",
            label: "상태",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "사용", value: "ACTIVE" },
              { label: "비활성", value: "DISABLED" },
            ],
          },
          {
            name: "sensitive",
            label: "민감 권한",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "포함", value: "true" },
              { label: "미포함", value: "false" },
            ],
          },
        ]}
        values={filters}
        onChange={(name, value) => {
          setFilters((current) => ({ ...current, [name]: value }));
          setPageState((current) => ({ ...current, page: 0 }));
        }}
        onSubmit={() => setPageState((current) => ({ ...current, page: 0 }))}
        onReset={() => {
          setFilters({ keyword: "", status: "", sensitive: "" });
          setPageState((current) => ({ ...current, page: 0 }));
        }}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="역할명" column="name" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="표시명" column="displayName" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>설명</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="권한 수" column="permissionCount" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="민감" column="sensitive" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="enabled" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {pagedRoles.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
              {pagedRoles.map((role) => (
                <tr key={role.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <button type="button" className="font-medium text-primary hover:underline" onClick={() => void openRoleDetail(role)}>
                      {role.name}
                    </button>
                  </td>
                  <td className={adminCellClassName}>{display(role.displayName)}</td>
                  <td className={adminCellClassName}>{display(role.description)}</td>
                  <td className={adminCellClassName}>{role.permissionCount}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={role.sensitive ? "warning" : "default"}>{role.sensitive ? "포함" : "미포함"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(role.enabled)}>{statusLabel(role.enabled)}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <div className="flex justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openRoleModal("edit", role)}>수정</Button>
                      <Button size="sm" variant="destructive" disabled={role.systemRole || !role.enabled} onClick={() => setDisableTarget(role)}>
                        비활성화
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={listPageState} onPageChange={(page) => setPageState((current) => ({ ...current, page }))} />
        </CardContent>
      </Card>

      <AdminCrudModal
        open={modalMode !== null}
        title={modalMode === "create" ? "역할 추가" : "역할 수정"}
        description="역할 이름은 ROLE_ 접두사를 포함해 입력합니다."
        onOpenChange={(open) => {
          if (!open) setModalMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalMode(null)}>취소</Button>
            <Button type="button" onClick={() => void saveRole()}>저장</Button>
          </>
        }
      >
        <Field label="역할명" value={roleForm.name} disabled={modalMode === "edit"} onChange={(value) => setRoleForm((current) => ({ ...current, name: value }))} />
        <Field label="표시명" value={roleForm.displayName} onChange={(value) => setRoleForm((current) => ({ ...current, displayName: value }))} />
        <Field label="설명" value={roleForm.description} onChange={(value) => setRoleForm((current) => ({ ...current, description: value }))} />
        <SelectField
          label="사용 여부"
          value={roleForm.enabled}
          options={[{ label: "사용", value: "true" }, { label: "비활성", value: "false" }]}
          onChange={(value) => setRoleForm((current) => ({ ...current, enabled: value }))}
        />
        <SelectField
          label="시스템 역할"
          value={roleForm.systemRole}
          options={[{ label: "아니오", value: "false" }, { label: "예", value: "true" }]}
          onChange={(value) => setRoleForm((current) => ({ ...current, systemRole: value }))}
        />
        <Field label="사유" value={roleForm.reason} onChange={(value) => setRoleForm((current) => ({ ...current, reason: value }))} />
      </AdminCrudModal>

      <AdminCrudModal
        open={detail !== null}
        title={detail ? `${detail.role.name} 상세` : "역할 상세"}
        description="역할에 연결된 권한을 조회하고 추가 또는 회수합니다."
        contentClassName="sm:max-w-[880px]"
        onOpenChange={(open) => {
          if (!open) setDetail(null);
        }}
      >
        {detail ? (
          <div className="space-y-4">
            <div className="grid gap-3 rounded-lg border p-3 text-sm md:grid-cols-4">
              <Info label="표시명" value={detail.role.displayName || "-"} />
              <Info label="상태" value={statusLabel(detail.role.enabled)} />
              <Info label="민감 권한" value={detail.role.sensitive ? "포함" : "미포함"} />
              <Info label="수정일" value={detail.role.updatedAt ? formatSecurityDateTime(detail.role.updatedAt) : "-"} />
            </div>

            <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
              <SelectField
                label="추가할 권한"
                value={permissionForm.permissionId}
                options={[
                  { label: "권한 선택", value: "" },
                  ...assignablePermissions.map((permission) => ({
                    label: `${permission.code} - ${permission.name}`,
                    value: String(permission.id),
                  })),
                ]}
                onChange={(value) => setPermissionForm((current) => ({ ...current, permissionId: value }))}
              />
              <Field label="추가 사유" value={permissionForm.reason} onChange={(value) => setPermissionForm((current) => ({ ...current, reason: value }))} />
              <div className="flex items-end">
                <Button type="button" className="w-full" onClick={() => void assignPermission()}>권한 추가</Button>
              </div>
            </div>

            <div className="overflow-x-auto rounded-lg border">
              <table className={adminTableClassName}>
                <thead className={adminTheadClassName}>
                  <tr>
                    <th className={adminCellClassName}>권한 코드</th>
                    <th className={adminCellClassName}>권한명</th>
                    <th className={adminCellClassName}>카테고리</th>
                    <th className={adminCellClassName}>민감</th>
                    <th className={adminCellClassName}>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.permissions.length === 0 ? <AdminEmptyRow colSpan={5} /> : null}
                  {detail.permissions.map((permission) => (
                    <tr key={permission.id} className={adminRowClassName}>
                      <td className={adminCellClassName}>{permission.code}</td>
                      <td className={adminCellClassName}>{permission.name}</td>
                      <td className={adminCellClassName}>{display(permission.category)}</td>
                      <td className={adminCellClassName}>
                        <AdminBadge tone={permission.sensitive ? "warning" : "default"}>{permission.sensitive ? "민감" : "일반"}</AdminBadge>
                      </td>
                      <td className={adminCellClassName}>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={async () => {
                            setDetail(await removeAdminRolePermission(detail.role.id, permission.id, "역할 권한 회수"));
                            await load();
                          }}
                        >
                          회수
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </AdminCrudModal>

      <AdminConfirmDialog
        open={disableTarget !== null}
        title="역할 비활성화"
        description={`${disableTarget?.name ?? ""} 역할을 비활성화합니다. 이미 부여된 권한 영향 범위를 확인해 주세요.`}
        confirmLabel="비활성화"
        destructive
        onOpenChange={(open) => {
          if (!open) setDisableTarget(null);
        }}
        onConfirm={() => {
          if (!disableTarget) return;
          void disableAdminRole(disableTarget.id).then(load);
        }}
      />
    </AdminPageShell>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 font-medium">{value}</p>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  disabled = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <Input value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)} />
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
