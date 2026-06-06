import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminPermission, AdminPermissionRequest } from "@/models/AdminModels";
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
  compareText,
  containsText as contains,
  displayValue as display,
  enabledStatusLabel as statusLabel,
  statusTone,
} from "@/pages/admin/adminUi";
import {
  createAdminPermission,
  deleteAdminPermission,
  getAdminPermissions,
  updateAdminPermission,
} from "@/services/AdminService";

const blankPermissionForm = {
  code: "",
  name: "",
  category: "",
  description: "",
  sensitive: "false",
  enabled: "true",
  reason: "",
};

export default function PermissionManagementPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [permissions, setPermissions] = useState<AdminPermission[]>([]);
  const [filters, setFilters] = useState({ keyword: "", category: "", status: "", sensitive: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "code", direction: "ASC" });
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editPermission, setEditPermission] = useState<AdminPermission | null>(null);
  const [permissionForm, setPermissionForm] = useState(blankPermissionForm);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<number[]>([]);
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);

  const load = async () => {
    const next = await getAdminPermissions();
    setPermissions(next);
    setSelectedPermissionIds([]);
    setEditPermission((current) => (current ? next.find((permission) => permission.id === current.id) ?? current : current));
  };

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin]);

  const categoryOptions = useMemo(() => {
    const categories = Array.from(new Set(permissions.map((permission) => permission.category).filter(Boolean) as string[]));
    return [{ label: "전체", value: "" }, ...categories.map((category) => ({ label: category, value: category }))];
  }, [permissions]);

  const filteredPermissions = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = permissions.filter((permission) => {
      const status = permission.enabled ? "ACTIVE" : "DISABLED";
      return (!keyword || [permission.code, permission.name, permission.category, permission.description].some((value) => contains(value, keyword)))
        && (!filters.category || permission.category === filters.category)
        && (!filters.status || status === filters.status)
        && (!filters.sensitive || String(permission.sensitive) === filters.sensitive);
    });

    return [...filtered].sort((left, right) => {
      const direction = sortState.direction === "ASC" ? 1 : -1;
      const result = (() => {
        switch (sortState.sort) {
          case "name":
            return compareText(left.name, right.name);
          case "category":
            return compareText(left.category, right.category);
          case "sensitive":
            return Number(left.sensitive) - Number(right.sensitive);
          case "enabled":
            return Number(left.enabled) - Number(right.enabled);
          case "code":
          default:
            return compareText(left.code, right.code);
        }
      })();
      return result * direction;
    });
  }, [permissions, filters, sortState]);

  const listPageState = {
    ...pageState,
    totalElements: filteredPermissions.length,
    totalPages: Math.max(Math.ceil(filteredPermissions.length / pageState.size), 1),
  };
  const pagedPermissions = filteredPermissions.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size);
  const allPageSelected = pagedPermissions.length > 0 && pagedPermissions.every((permission) => selectedPermissionIds.includes(permission.id));
  const selectedPermissions = permissions.filter((permission) => selectedPermissionIds.includes(permission.id));

  const handleSort = (column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
    setPageState((current) => ({ ...current, page: 0 }));
  };

  const openPermissionModal = (mode: "create" | "edit", permission?: AdminPermission) => {
    setModalMode(mode);
    setEditPermission(permission ?? null);
    setPermissionForm(
      permission
        ? {
            code: permission.code,
            name: permission.name,
            category: permission.category || "",
            description: permission.description || "",
            sensitive: String(permission.sensitive),
            enabled: String(permission.enabled),
            reason: "",
          }
        : blankPermissionForm,
    );
  };

  const savePermission = async () => {
    const payload: AdminPermissionRequest = {
      code: permissionForm.code.trim(),
      name: permissionForm.name,
      category: permissionForm.category,
      description: permissionForm.description,
      sensitive: permissionForm.sensitive === "true",
      enabled: permissionForm.enabled === "true",
      reason: permissionForm.reason || (modalMode === "create" ? "권한 생성" : "권한 수정"),
    };

    if (modalMode === "edit" && editPermission) {
      await updateAdminPermission(editPermission.id, payload);
    } else {
      await createAdminPermission(payload);
    }
    setModalMode(null);
    setPermissionForm(blankPermissionForm);
    await load();
  };

  const togglePermission = (id: number, checked: boolean) => {
    setSelectedPermissionIds((current) => checked ? Array.from(new Set([...current, id])) : current.filter((item) => item !== id));
  };

  const togglePage = (checked: boolean) => {
    setSelectedPermissionIds(checked ? pagedPermissions.map((permission) => permission.id) : []);
  };

  const runBulkDelete = async () => {
    await Promise.all(selectedPermissions.map((permission) => deleteAdminPermission(permission.id)));
    setSelectedPermissionIds([]);
    setBulkDeleteOpen(false);
    await load();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="권한 관리"
      description="역할에 매핑할 세부 권한을 생성하고 민감 권한 여부를 관리합니다."
      actions={<Button type="button" onClick={() => openPermissionModal("create")}>권한 추가</Button>}
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "권한 검색", placeholder: "코드, 이름, 설명" },
          { name: "category", label: "카테고리", type: "select", options: categoryOptions },
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
              { label: "민감", value: "true" },
              { label: "일반", value: "false" },
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
          setFilters({ keyword: "", category: "", status: "", sensitive: "" });
          setPageState((current) => ({ ...current, page: 0 }));
        }}
      />

      <AdminBulkActionBar selectedLabel={`선택 ${selectedPermissionIds.length}건`}>
        <Button type="button" variant="destructive" disabled={selectedPermissionIds.length === 0} onClick={() => setBulkDeleteOpen(true)}>선택 삭제</Button>
      </AdminBulkActionBar>

      <AdminTableCard>
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <input type="checkbox" aria-label="현재 페이지 권한 전체 선택" checked={allPageSelected} onChange={(event) => togglePage(event.target.checked)} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="권한 코드" column="code" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="권한명" column="name" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="카테고리" column="category" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>설명</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="민감" column="sensitive" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="enabled" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>수정일</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {pagedPermissions.length === 0 ? <AdminEmptyRow colSpan={9} /> : null}
              {pagedPermissions.map((permission) => (
                <tr key={permission.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <input
                      type="checkbox"
                      aria-label={`${permission.code} 선택`}
                      checked={selectedPermissionIds.includes(permission.id)}
                      onChange={(event) => togglePermission(permission.id, event.target.checked)}
                    />
                  </td>
                  <td className={adminCellClassName}>{permission.code}</td>
                  <td className={adminCellClassName}>{permission.name}</td>
                  <td className={adminCellClassName}>{display(permission.category)}</td>
                  <td className={adminCellClassName}>{display(permission.description)}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={permission.sensitive ? "warning" : "default"}>{permission.sensitive ? "민감" : "일반"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(permission.enabled)}>{statusLabel(permission.enabled)}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{permission.updatedAt ? formatSecurityDateTime(permission.updatedAt) : "-"}</td>
                  <td className={adminCellClassName}>
                    <div className="flex justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openPermissionModal("edit", permission)}>수정</Button>
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
        title={modalMode === "create" ? "권한 추가" : "권한 수정"}
        description="권한 코드는 예: ADMIN_USERS_READ 처럼 메뉴와 행위를 함께 표현합니다."
        onOpenChange={(open) => {
          if (!open) setModalMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalMode(null)}>취소</Button>
            <Button type="button" onClick={() => void savePermission()}>저장</Button>
          </>
        }
      >
        <AdminFormField label="권한 코드" value={permissionForm.code} disabled={modalMode === "edit"} onChange={(value) => setPermissionForm((current) => ({ ...current, code: value }))} />
        <AdminFormField label="권한명" value={permissionForm.name} onChange={(value) => setPermissionForm((current) => ({ ...current, name: value }))} />
        <AdminFormField label="카테고리" value={permissionForm.category} onChange={(value) => setPermissionForm((current) => ({ ...current, category: value }))} />
        <AdminFormField label="설명" value={permissionForm.description} onChange={(value) => setPermissionForm((current) => ({ ...current, description: value }))} />
        <AdminSelectField
          label="민감 권한"
          value={permissionForm.sensitive}
          options={[{ label: "일반", value: "false" }, { label: "민감", value: "true" }]}
          onChange={(value) => setPermissionForm((current) => ({ ...current, sensitive: value }))}
        />
        <AdminSelectField
          label="사용 여부"
          value={permissionForm.enabled}
          options={[{ label: "사용", value: "true" }, { label: "비활성", value: "false" }]}
          onChange={(value) => setPermissionForm((current) => ({ ...current, enabled: value }))}
        />
        <AdminFormField label="사유" value={permissionForm.reason} onChange={(value) => setPermissionForm((current) => ({ ...current, reason: value }))} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={bulkDeleteOpen}
        title="권한 삭제"
        description={`${selectedPermissionIds.length}개 권한을 삭제합니다. 역할에 연결된 권한이면 먼저 매핑을 회수해 주세요.`}
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
