import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AdminGroup, AdminGroupDetail, AdminGroupRequest, AdminRole } from "@/models/AdminModels";
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
  addAdminGroupMember,
  assignAdminGroupRole,
  createAdminGroup,
  disableAdminGroup,
  getAdminGroupDetail,
  getAdminGroups,
  getAdminRoles,
  getAdminUsers,
  removeAdminGroupMember,
  removeAdminGroupRole,
  updateAdminGroup,
} from "@/services/AdminService";

const blankGroup = { name: "", type: "STATIC", ownerUsername: "", description: "", reason: "" };
const contains = (value: string | number | null | undefined, keyword: string) =>
  String(value ?? "").toLowerCase().includes(keyword.trim().toLowerCase());
const compareText = (left?: string | null, right?: string | null) => String(left ?? "").localeCompare(String(right ?? ""));
const display = (value?: string | number | null) => (value === null || value === undefined || value === "" ? "-" : String(value));

export default function GroupManagementPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [groups, setGroups] = useState<AdminGroup[]>([]);
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [filters, setFilters] = useState({ keyword: "", type: "", status: "", owner: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "name", direction: "ASC" });
  const [groupForm, setGroupForm] = useState(blankGroup);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editGroup, setEditGroup] = useState<AdminGroup | null>(null);
  const [detailGroup, setDetailGroup] = useState<AdminGroupDetail | null>(null);
  const [disableTarget, setDisableTarget] = useState<AdminGroup | null>(null);
  const [member, setMember] = useState("");
  const [roleName, setRoleName] = useState("");
  const [roleSensitiveReason, setRoleSensitiveReason] = useState("");
  const [detailMessage, setDetailMessage] = useState("");

  const loadGroups = async () => {
    const next = await getAdminGroups();
    setGroups(next);
    setDetailGroup((current) => {
      const nextGroup = current ? next.find((item) => item.id === current.group.id) : null;
      return current && nextGroup ? { ...current, group: nextGroup } : current;
    });
    setEditGroup((current) => (current ? next.find((item) => item.id === current.id) ?? current : current));
  };

  const loadRoles = async () => {
    setRoles(await getAdminRoles());
  };

  useEffect(() => {
    if (isAdmin) {
      void loadGroups().catch(() => undefined);
      void loadRoles().catch(() => undefined);
    }
  }, [isAdmin]);

  const filteredGroups = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = groups.filter((group) => {
      const status = group.enabled ? "ACTIVE" : "DISABLED";
      return (!keyword || [group.name, group.type, group.ownerUsername, group.description].some((value) => contains(value, keyword)))
        && (!filters.type || group.type === filters.type)
        && (!filters.status || status === filters.status)
        && (!filters.owner || contains(group.ownerUsername, filters.owner));
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

  const groupListPageState = {
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

  const openGroupModal = (mode: "create" | "edit", group?: AdminGroup) => {
    setModalMode(mode);
    setEditGroup(group ?? null);
    setGroupForm(
      group
        ? {
            name: group.name,
            type: group.type,
            ownerUsername: group.ownerUsername || "",
            description: group.description || "",
            reason: "",
          }
        : blankGroup,
    );
  };

  const openGroupDetail = async (group: AdminGroup) => {
    setDetailGroup(await getAdminGroupDetail(group.id));
    setMember("");
    setRoleName("");
    setRoleSensitiveReason("");
    setDetailMessage("");
  };

  const selectedRole = roles.find((role) => role.name === roleName);

  const validateExistingUser = async (username: string) => {
    const trimmed = username.trim();
    if (!trimmed) {
      setDetailMessage("추가할 사용자 ID를 입력하세요.");
      return false;
    }
    const page = await getAdminUsers({ keyword: trimmed, page: 0, size: 10, sort: "username", direction: "ASC" });
    const exactUser = page.content.find((item) => item.username === trimmed && !item.deleted);
    if (!exactUser) {
      setDetailMessage("기존에 등록된 사용자 ID만 그룹에 추가할 수 있습니다.");
      return false;
    }
    return true;
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="그룹 관리"
      description="그룹을 목록으로 조회하고, 상세 모달에서 구성원과 역할을 관리합니다."
      actions={
        <Button type="button" onClick={() => openGroupModal("create")}>
          그룹 추가
        </Button>
      }
    >
      <AdminFilters
        hint="그룹 목록 필터 적용"
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
          { name: "owner", label: "소유자", placeholder: "소유자 사용자 ID" },
        ]}
        values={filters}
        onChange={(name, value) => {
          setFilters((current) => ({ ...current, [name]: value }));
          setPageState((current) => ({ ...current, page: 0 }));
        }}
        onSubmit={() => setPageState((current) => ({ ...current, page: 0 }))}
        onReset={() => {
          setFilters({ keyword: "", type: "", status: "", owner: "" });
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
                    <button type="button" className="font-medium text-primary hover:underline" onClick={() => void openGroupDetail(group)}>
                      {group.name}
                    </button>
                  </td>
                  <td className={adminCellClassName}>{group.type}</td>
                  <td className={adminCellClassName}>{group.ownerUsername || "-"}</td>
                  <td className={adminCellClassName}>{group.userCount}</td>
                  <td className={adminCellClassName}>{group.roleCount}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(group.enabled)}>{group.enabled ? "ACTIVE" : "DISABLED"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <div className="flex justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openGroupModal("edit", group)}>
                        수정
                      </Button>
                      <Button size="sm" variant="destructive" onClick={() => setDisableTarget(group)}>
                        삭제
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={groupListPageState} onPageChange={(page) => setPageState((current) => ({ ...current, page }))} />
        </CardContent>
      </Card>

      <AdminCrudModal
        open={modalMode !== null}
        title={modalMode === "create" ? "그룹 추가" : "그룹 수정"}
        description="그룹 기본 정보와 소유자를 입력합니다."
        onOpenChange={(open) => {
          if (!open) setModalMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalMode(null)}>
              취소
            </Button>
            <Button
              type="button"
              onClick={async () => {
                const payload: AdminGroupRequest = {
                  name: groupForm.name,
                  type: groupForm.type,
                  ownerUsername: groupForm.ownerUsername || null,
                  description: groupForm.description || null,
                  enabled: true,
                  reason: groupForm.reason || (modalMode === "create" ? "그룹 생성" : "그룹 수정"),
                };
                if (modalMode === "edit" && editGroup) {
                  await updateAdminGroup(editGroup.id, payload);
                } else {
                  await createAdminGroup(payload);
                }
                setModalMode(null);
                setGroupForm(blankGroup);
                await loadGroups();
              }}
            >
              저장
            </Button>
          </>
        }
      >
        <Field label="그룹명" value={groupForm.name} onChange={(value) => setGroupForm((current) => ({ ...current, name: value }))} />
        <SelectField
          label="유형"
          value={groupForm.type}
          options={[
            { label: "정적", value: "STATIC" },
            { label: "동적", value: "DYNAMIC" },
          ]}
          onChange={(value) => setGroupForm((current) => ({ ...current, type: value }))}
        />
        <Field label="소유자 사용자 ID" value={groupForm.ownerUsername} onChange={(value) => setGroupForm((current) => ({ ...current, ownerUsername: value }))} />
        <Field label="설명" value={groupForm.description} onChange={(value) => setGroupForm((current) => ({ ...current, description: value }))} />
        <Field label="사유" value={groupForm.reason} onChange={(value) => setGroupForm((current) => ({ ...current, reason: value }))} />
      </AdminCrudModal>

      <AdminCrudModal
        open={detailGroup !== null}
        title={detailGroup ? `${detailGroup.group.name} 상세` : "그룹 상세"}
        description="그룹 구성원, 역할, 기본 정보를 조회하고 관리합니다."
        contentClassName="sm:max-w-[900px]"
        onOpenChange={(open) => {
          if (!open) {
            setDetailGroup(null);
            setMember("");
            setRoleName("");
            setDetailMessage("");
          }
        }}
      >
        {detailGroup ? (
          <div className="space-y-5">
            <div className="grid gap-3 rounded-lg border p-3 text-sm sm:grid-cols-2 lg:grid-cols-5">
              <Info label="그룹명" value={detailGroup.group.name} />
              <Info label="유형" value={detailGroup.group.type} />
              <Info label="소유자" value={detailGroup.group.ownerUsername || "-"} />
              <Info label="구성원" value={`${detailGroup.group.userCount}명`} />
              <Info label="역할" value={`${detailGroup.group.roleCount}개`} />
            </div>

            {detailMessage ? <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{detailMessage}</p> : null}

            <div className="grid gap-4 lg:grid-cols-2">
              <section className="space-y-3 rounded-lg border p-3">
                <div>
                  <h3 className="font-medium">구성원</h3>
                  <p className="text-xs text-muted-foreground">기존 사용자 ID만 구성원으로 추가할 수 있습니다.</p>
                </div>
                <div className="flex gap-2">
                  <Input placeholder="추가할 사용자 ID" value={member} onChange={(event) => setMember(event.target.value)} />
                  <Button
                    type="button"
                    onClick={async () => {
                      if (!detailGroup || !(await validateExistingUser(member))) return;
                      setDetailGroup(await addAdminGroupMember(detailGroup.group.id, { username: member.trim(), reason: "그룹 구성원 추가" }));
                      setMember("");
                      setDetailMessage("");
                      await loadGroups();
                    }}
                  >
                    추가
                  </Button>
                </div>
                <div className="overflow-x-auto rounded-lg border">
                  <table className="w-full min-w-[420px] text-sm">
                    <thead className={adminTheadClassName}>
                      <tr>
                        <th className={adminCellClassName}>사용자 ID</th>
                        <th className={adminCellClassName}>이름</th>
                        <th className={adminCellClassName}>작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detailGroup.members.length === 0 ? <AdminEmptyRow colSpan={3} /> : null}
                      {detailGroup.members.map((item) => (
                        <tr key={item.username} className={adminRowClassName}>
                          <td className={adminCellClassName}>{item.username}</td>
                          <td className={adminCellClassName}>{display(item.name)}</td>
                          <td className={adminCellClassName}>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={async () => {
                                setDetailGroup(await removeAdminGroupMember(detailGroup.group.id, item.username, "그룹 구성원 제거"));
                                await loadGroups();
                              }}
                            >
                              삭제
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>

              <section className="space-y-3 rounded-lg border p-3">
                <div>
                  <h3 className="font-medium">역할</h3>
                  <p className="text-xs text-muted-foreground">그룹에 공통 역할을 할당하거나 회수합니다.</p>
                </div>
                <div className="grid gap-2 md:grid-cols-[1fr_1fr_auto]">
                  <SelectField
                    label="할당할 역할"
                    value={roleName}
                    options={[
                      { label: "역할 선택", value: "" },
                      ...roles.filter((role) => role.enabled).map((role) => ({ label: `${role.name} ${role.displayName ? `(${role.displayName})` : ""}`, value: role.name })),
                    ]}
                    onChange={(value) => {
                      setRoleName(value);
                      setRoleSensitiveReason("");
                    }}
                  />
                  <Field
                    label="민감 권한 사유"
                    value={roleSensitiveReason}
                    disabled={!selectedRole?.sensitive}
                    onChange={setRoleSensitiveReason}
                  />
                  <Button
                    type="button"
                    className="self-end"
                    onClick={async () => {
                      if (!detailGroup || !roleName.trim()) return;
                      setDetailGroup(await assignAdminGroupRole(detailGroup.group.id, {
                        roleName: roleName.trim(),
                        reason: "그룹 역할 할당",
                        sensitiveReason: selectedRole?.sensitive ? roleSensitiveReason : undefined,
                      }));
                      setRoleName("");
                      setRoleSensitiveReason("");
                      await loadGroups();
                    }}
                  >
                    할당
                  </Button>
                </div>
                <div className="overflow-x-auto rounded-lg border">
                  <table className="w-full min-w-[360px] text-sm">
                    <thead className={adminTheadClassName}>
                      <tr>
                        <th className={adminCellClassName}>역할명</th>
                        <th className={adminCellClassName}>작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detailGroup.roles.length === 0 ? <AdminEmptyRow colSpan={2} /> : null}
                      {detailGroup.roles.map((role) => (
                        <tr key={role.roleId} className={adminRowClassName}>
                          <td className={adminCellClassName}>{role.roleName}</td>
                          <td className={adminCellClassName}>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={async () => {
                                setDetailGroup(await removeAdminGroupRole(detailGroup.group.id, role.roleId, "그룹 역할 회수"));
                                await loadGroups();
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
              </section>
            </div>
          </div>
        ) : null}
      </AdminCrudModal>

      <AdminConfirmDialog
        open={disableTarget !== null}
        title="그룹 삭제"
        description={`${disableTarget?.name ?? ""} 그룹을 비활성화합니다. 구성원과 역할 할당 상태를 먼저 확인하세요.`}
        confirmLabel="비활성화"
        destructive
        onOpenChange={(open) => {
          if (!open) setDisableTarget(null);
        }}
        onConfirm={() => {
          if (!disableTarget) return;
          void disableAdminGroup(disableTarget.id, "그룹 비활성화").then(loadGroups).catch(() => undefined);
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
