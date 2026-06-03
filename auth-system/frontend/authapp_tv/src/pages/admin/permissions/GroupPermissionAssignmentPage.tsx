import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AdminGroup, AdminGroupDetail, AdminRole } from "@/models/AdminModels";
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
  assignAdminGroupRole,
  getAdminGroupDetail,
  getAdminGroups,
  getAdminRoles,
  removeAdminGroupRole,
} from "@/services/AdminService";

const blankAssignment = {
  roleName: "",
  reason: "",
  sensitiveReason: "",
};

const contains = (value: string | number | boolean | null | undefined, keyword: string) =>
  String(value ?? "").toLowerCase().includes(keyword.trim().toLowerCase());
const compareText = (left?: string | null, right?: string | null) => String(left ?? "").localeCompare(String(right ?? ""));
const display = (value?: string | number | null) => (value === null || value === undefined || value === "" ? "-" : String(value));
const statusLabel = (enabled: boolean) => (enabled ? "활성" : "비활성");

export default function GroupPermissionAssignmentPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [groups, setGroups] = useState<AdminGroup[]>([]);
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [filters, setFilters] = useState({ keyword: "", type: "", status: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "name", direction: "ASC" });
  const [detail, setDetail] = useState<AdminGroupDetail | null>(null);
  const [assignmentForm, setAssignmentForm] = useState(blankAssignment);
  const [revokeTarget, setRevokeTarget] = useState<{ groupId: number; roleId: number; roleName: string } | null>(null);

  const roleOptions = useMemo(
    () => [{ label: "역할 선택", value: "" }, ...roles.filter((role) => role.enabled).map((role) => ({ label: `${role.name} ${role.displayName ? `(${role.displayName})` : ""}`, value: role.name }))],
    [roles],
  );
  const selectedRole = roles.find((role) => role.name === assignmentForm.roleName);

  const load = async () => {
    const [nextGroups, nextRoles] = await Promise.all([getAdminGroups(), getAdminRoles()]);
    setGroups(nextGroups);
    setRoles(nextRoles);
    if (detail) {
      const stillExists = nextGroups.some((group) => group.id === detail.group.id);
      if (stillExists) setDetail(await getAdminGroupDetail(detail.group.id));
    }
  };

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin]);

  const filteredGroups = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = groups.filter((group) => {
      const status = group.enabled ? "ACTIVE" : "DISABLED";
      return (!keyword || [group.name, group.type, group.ownerUsername, group.description].some((value) => contains(value, keyword)))
        && (!filters.type || group.type === filters.type)
        && (!filters.status || status === filters.status);
    });

    return [...filtered].sort((left, right) => {
      const direction = sortState.direction === "ASC" ? 1 : -1;
      const result = (() => {
        switch (sortState.sort) {
          case "type":
            return compareText(left.type, right.type);
          case "ownerUsername":
            return compareText(left.ownerUsername, right.ownerUsername);
          case "userCount":
            return left.userCount - right.userCount;
          case "roleCount":
            return left.roleCount - right.roleCount;
          case "enabled":
            return Number(left.enabled) - Number(right.enabled);
          case "name":
          default:
            return compareText(left.name, right.name);
        }
      })();
      return result * direction;
    });
  }, [groups, filters, sortState]);

  const listPageState = {
    ...pageState,
    totalElements: filteredGroups.length,
    totalPages: Math.max(Math.ceil(filteredGroups.length / pageState.size), 1),
  };
  const pagedGroups = filteredGroups.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size);

  const handleSort = (column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
    setPageState((current) => ({ ...current, page: 0 }));
  };

  const openDetail = async (group: AdminGroup) => {
    setDetail(await getAdminGroupDetail(group.id));
    setAssignmentForm(blankAssignment);
  };

  const saveAssignment = async () => {
    if (!detail || !assignmentForm.roleName) return;
    setDetail(await assignAdminGroupRole(detail.group.id, {
      roleName: assignmentForm.roleName,
      reason: assignmentForm.reason || "그룹 역할 부여",
      sensitiveReason: selectedRole?.sensitive ? assignmentForm.sensitiveReason : undefined,
    }));
    setAssignmentForm(blankAssignment);
    await load();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="그룹 권한 할당"
      description="그룹별 공통 역할을 조회하고 부여 또는 회수합니다."
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "그룹 검색", placeholder: "그룹명, 설명, 소유자" },
          {
            name: "type",
            label: "유형",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "정적", value: "STATIC" },
              { label: "동적", value: "DYNAMIC" },
            ],
          },
          {
            name: "status",
            label: "상태",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "활성", value: "ACTIVE" },
              { label: "비활성", value: "DISABLED" },
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
          setFilters({ keyword: "", type: "", status: "" });
          setPageState((current) => ({ ...current, page: 0 }));
        }}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="그룹명" column="name" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="유형" column="type" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="소유자" column="ownerUsername" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자 수" column="userCount" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="역할 수" column="roleCount" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="enabled" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {pagedGroups.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
              {pagedGroups.map((group) => (
                <tr key={group.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <button type="button" className="font-medium text-primary hover:underline" onClick={() => void openDetail(group)}>
                      {group.name}
                    </button>
                  </td>
                  <td className={adminCellClassName}>{group.type}</td>
                  <td className={adminCellClassName}>{display(group.ownerUsername)}</td>
                  <td className={adminCellClassName}>{group.userCount}</td>
                  <td className={adminCellClassName}>{group.roleCount}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(group.enabled)}>{statusLabel(group.enabled)}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <Button size="sm" variant="outline" onClick={() => void openDetail(group)}>역할 관리</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={listPageState} onPageChange={(page) => setPageState((current) => ({ ...current, page }))} />
        </CardContent>
      </Card>

      <AdminCrudModal
        open={detail !== null}
        title={detail ? `${detail.group.name} 역할 관리` : "그룹 역할 관리"}
        description="그룹에 부여된 역할은 소속 사용자에게 공통 권한 기준으로 사용됩니다."
        contentClassName="sm:max-w-[820px]"
        onOpenChange={(open) => {
          if (!open) setDetail(null);
        }}
      >
        {detail ? (
          <div className="space-y-4">
            <div className="grid gap-3 rounded-lg border p-3 text-sm md:grid-cols-4">
              <Info label="그룹명" value={detail.group.name} />
              <Info label="유형" value={detail.group.type} />
              <Info label="구성원" value={`${detail.group.userCount}명`} />
              <Info label="역할" value={`${detail.group.roleCount}개`} />
            </div>

            <div className="grid gap-3 md:grid-cols-[1fr_1fr_1fr_auto]">
              <SelectField
                label="역할"
                value={assignmentForm.roleName}
                options={roleOptions}
                onChange={(value) => setAssignmentForm((current) => ({ ...current, roleName: value }))}
              />
              <Field label="부여 사유" value={assignmentForm.reason} onChange={(value) => setAssignmentForm((current) => ({ ...current, reason: value }))} />
              <Field
                label="민감 권한 사유"
                value={assignmentForm.sensitiveReason}
                disabled={!selectedRole?.sensitive}
                onChange={(value) => setAssignmentForm((current) => ({ ...current, sensitiveReason: value }))}
              />
              <div className="flex items-end">
                <Button type="button" className="w-full" onClick={() => void saveAssignment()}>부여</Button>
              </div>
            </div>

            <div className="overflow-x-auto rounded-lg border">
              <table className={adminTableClassName}>
                <thead className={adminTheadClassName}>
                  <tr>
                    <th className={adminCellClassName}>역할명</th>
                    <th className={adminCellClassName}>민감</th>
                    <th className={adminCellClassName}>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.roles.length === 0 ? <AdminEmptyRow colSpan={3} /> : null}
                  {detail.roles.map((role) => {
                    const roleMeta = roles.find((item) => item.id === role.roleId);
                    return (
                      <tr key={role.roleId} className={adminRowClassName}>
                        <td className={adminCellClassName}>{role.roleName}</td>
                        <td className={adminCellClassName}>
                          <AdminBadge tone={roleMeta?.sensitive ? "warning" : "default"}>{roleMeta?.sensitive ? "포함" : "미포함"}</AdminBadge>
                        </td>
                        <td className={adminCellClassName}>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setRevokeTarget({ groupId: detail.group.id, roleId: role.roleId, roleName: role.roleName })}
                          >
                            회수
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </AdminCrudModal>

      <AdminConfirmDialog
        open={revokeTarget !== null}
        title="그룹 역할 회수"
        description={`${detail?.group.name ?? ""} 그룹에서 ${revokeTarget?.roleName ?? ""} 역할을 회수합니다.`}
        confirmLabel="회수"
        destructive
        onOpenChange={(open) => {
          if (!open) setRevokeTarget(null);
        }}
        onConfirm={() => {
          if (!revokeTarget) return;
          void removeAdminGroupRole(revokeTarget.groupId, revokeTarget.roleId, "그룹 역할 회수").then(async (nextDetail) => {
            setDetail(nextDetail);
            await load();
          });
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
