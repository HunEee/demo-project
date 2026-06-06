import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminPermission, AdminRole, AdminRoleDetail, AdminRoleRequest } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminBulkActionBar,
  AdminConfirmDialog,
  AdminCrudModal,
  AdminEmptyRow,
  AdminFormField,
  AdminInfoItem,
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
  compareText,
  containsText as contains,
  displayValue as display,
  enabledStatusLabel as statusLabel,
  statusTone,
} from "@/pages/admin/adminUi";
import {
  assignAdminRolePermission,
  createAdminRole,
  deleteAdminRole,
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
  const [selectedRoleIds, setSelectedRoleIds] = useState<number[]>([]);
  const [bulkDisableOpen, setBulkDisableOpen] = useState(false);
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);
  const [detail, setDetail] = useState<AdminRoleDetail | null>(null);
  const [permissionForm, setPermissionForm] = useState(blankPermissionForm);

  const load = async () => {
    const [nextRoles, nextPermissions] = await Promise.all([getAdminRoles(), getAdminPermissions()]);
    setRoles(nextRoles);
    setPermissions(nextPermissions);
    setSelectedRoleIds([]);
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
  const selectableRoles = pagedRoles.filter((role) => !role.systemRole);
  const selectedRoles = roles.filter((role) => selectedRoleIds.includes(role.id));
  const allPageSelected = selectableRoles.length > 0 && selectableRoles.every((role) => selectedRoleIds.includes(role.id));
  const selectedEnabledRoleCount = selectedRoles.filter((role) => role.enabled).length;
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

  const toggleRole = (id: number, checked: boolean) => {
    setSelectedRoleIds((current) => checked ? Array.from(new Set([...current, id])) : current.filter((item) => item !== id));
  };

  const togglePage = (checked: boolean) => {
    setSelectedRoleIds(checked ? selectableRoles.map((role) => role.id) : []);
  };

  const runBulkDisable = async () => {
    const targets = selectedRoles.filter((role) => !role.systemRole && role.enabled);
    await Promise.all(targets.map((role) => disableAdminRole(role.id)));
    setSelectedRoleIds([]);
    setBulkDisableOpen(false);
    await load();
  };

  const runBulkDelete = async () => {
    const targets = selectedRoles.filter((role) => !role.systemRole);
    await Promise.all(targets.map((role) => deleteAdminRole(role.id)));
    setSelectedRoleIds([]);
    setBulkDeleteOpen(false);
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

      <AdminBulkActionBar selectedLabel={`선택 ${selectedRoleIds.length}건`}>
          <Button type="button" variant="outline" disabled={selectedEnabledRoleCount === 0} onClick={() => setBulkDisableOpen(true)}>선택 비활성화</Button>
          <Button type="button" variant="destructive" disabled={selectedRoleIds.length === 0} onClick={() => setBulkDeleteOpen(true)}>선택 삭제</Button>
      </AdminBulkActionBar>

      <AdminTableCard>
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <input type="checkbox" aria-label="현재 페이지 역할 전체 선택" checked={allPageSelected} onChange={(event) => togglePage(event.target.checked)} />
                </th>
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
              {pagedRoles.length === 0 ? <AdminEmptyRow colSpan={8} /> : null}
              {pagedRoles.map((role) => (
                <tr key={role.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <input
                      type="checkbox"
                      aria-label={`${role.name} 선택`}
                      disabled={role.systemRole}
                      checked={selectedRoleIds.includes(role.id)}
                      onChange={(event) => toggleRole(role.id, event.target.checked)}
                    />
                  </td>
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
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={listPageState} onPageChange={(page) => setPageState((current) => ({ ...current, page }))} />
      </AdminTableCard>

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
        <AdminFormField label="역할명" value={roleForm.name} disabled={modalMode === "edit"} onChange={(value) => setRoleForm((current) => ({ ...current, name: value }))} />
        <AdminFormField label="표시명" value={roleForm.displayName} onChange={(value) => setRoleForm((current) => ({ ...current, displayName: value }))} />
        <AdminFormField label="설명" value={roleForm.description} onChange={(value) => setRoleForm((current) => ({ ...current, description: value }))} />
        <AdminSelectField
          label="사용 여부"
          value={roleForm.enabled}
          options={[{ label: "사용", value: "true" }, { label: "비활성", value: "false" }]}
          onChange={(value) => setRoleForm((current) => ({ ...current, enabled: value }))}
        />
        <AdminSelectField
          label="시스템 역할"
          value={roleForm.systemRole}
          options={[{ label: "아니오", value: "false" }, { label: "예", value: "true" }]}
          onChange={(value) => setRoleForm((current) => ({ ...current, systemRole: value }))}
        />
        <AdminFormField label="사유" value={roleForm.reason} onChange={(value) => setRoleForm((current) => ({ ...current, reason: value }))} />
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
              <AdminInfoItem label="표시명" value={detail.role.displayName || "-"} />
              <AdminInfoItem label="상태" value={statusLabel(detail.role.enabled)} />
              <AdminInfoItem label="민감 권한" value={detail.role.sensitive ? "포함" : "미포함"} />
              <AdminInfoItem label="수정일" value={detail.role.updatedAt ? formatSecurityDateTime(detail.role.updatedAt) : "-"} />
            </div>

            <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
              <AdminSelectField
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
              <AdminFormField label="추가 사유" value={permissionForm.reason} onChange={(value) => setPermissionForm((current) => ({ ...current, reason: value }))} />
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
        open={bulkDisableOpen}
        title="역할 비활성화"
        description={`${selectedRoleIds.length}개 역할을 비활성화합니다. 시스템 역할과 이미 비활성화된 역할은 선택할 수 없습니다.`}
        confirmLabel="비활성화"
        destructive
        onOpenChange={(open) => {
          setBulkDisableOpen(open);
        }}
        onConfirm={() => {
          void runBulkDisable().catch(() => undefined);
        }}
      />
      <AdminConfirmDialog
        open={bulkDeleteOpen}
        title="역할 삭제"
        description={`${selectedRoleIds.length}개 역할을 삭제합니다. 사용자, 그룹, 권한 매핑이 남아 있는 역할은 삭제되지 않습니다. 운영 중인 역할은 삭제보다 비활성화를 사용하세요.`}
        confirmLabel="삭제"
        destructive
        onOpenChange={(open) => {
          setBulkDeleteOpen(open);
        }}
        onConfirm={() => {
          void runBulkDelete().catch(() => undefined);
        }}
      />
    </AdminPageShell>
  );
}
