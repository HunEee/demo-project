import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminFilterOptions, AdminUser } from "@/models/AdminModels";
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
  deleteAdminUser,
  disableAdminUser,
  getAdminFilterOptions,
  getAdminUsers,
  lockAdminUser,
  updateAdminUser,
} from "@/services/AdminService";

const blankEditForm = {
  email: "",
  name: "",
  locked: "false",
  enabled: "true",
  reason: "",
};

const accountStatus = (item: AdminUser) => {
  if (item.deleted) return "DELETED";
  return item.status || "ACTIVE";
};

const authMethodLabel = (item: AdminUser) => {
  if (item.social) return item.authMethod || "SOCIAL";
  return "PASSWORD";
};

export default function ExternalUsersPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", authMethod: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "createdAt", direction: "DESC" });
  const [selectedUsernames, setSelectedUsernames] = useState<string[]>([]);
  const [bulkAction, setBulkAction] = useState<"lock" | "disable" | "delete" | null>(null);
  const [editUser, setEditUser] = useState<AdminUser | null>(null);
  const [editForm, setEditForm] = useState(blankEditForm);
  const [formError, setFormError] = useState("");

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminUsers({
      keyword: filters.keyword,
      status: filters.status,
      authMethod: filters.authMethod,
      directOnly: true,
      page: nextPage,
      size: pageState.size,
      sort: nextSort.sort,
      direction: nextSort.direction,
    });
    setUsers(page.content);
    setSelectedUsernames([]);
    setPageState({
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
    });
  };

  useEffect(() => {
    if (isAdmin) {
      void load(0).catch(() => undefined);
      void getAdminFilterOptions().then(setFilterOptions).catch(() => undefined);
    }
  }, [isAdmin, filters.keyword, filters.status, filters.authMethod]);

  const handleSort = (column: string) => {
    const nextSort: SortState = {
      sort: column,
      direction: sortState.sort === column && sortState.direction === "DESC" ? "ASC" : "DESC",
    };
    setSortState(nextSort);
    void load(0, nextSort).catch(() => undefined);
  };

  const resetFilters = async () => {
    setFilters({ keyword: "", status: "", authMethod: "" });
    const page = await getAdminUsers({
      directOnly: true,
      page: 0,
      size: pageState.size,
      sort: sortState.sort,
      direction: sortState.direction,
    });
    setUsers(page.content);
    setSelectedUsernames([]);
    setPageState({
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
    });
  };

  const selectableUsers = users.filter((item) => item.username !== "admin" && !item.deleted);
  const selectedUsers = users.filter((item) => selectedUsernames.includes(item.username));
  const allPageSelected = selectableUsers.length > 0 && selectableUsers.every((item) => selectedUsernames.includes(item.username));

  const toggleUser = (username: string, checked: boolean) => {
    setSelectedUsernames((current) =>
      checked ? Array.from(new Set([...current, username])) : current.filter((item) => item !== username),
    );
  };

  const togglePage = (checked: boolean) => {
    setSelectedUsernames(checked ? selectableUsers.map((item) => item.username) : []);
  };

  const openEdit = (item: AdminUser) => {
    setEditUser(item);
    setEditForm({
      email: item.email || "",
      name: item.name || item.nickname || "",
      locked: String(item.locked),
      enabled: String(item.enabled),
      reason: "",
    });
    setFormError("");
  };

  const saveEdit = async () => {
    if (!editUser) return;
    await updateAdminUser(editUser.username, {
      email: editForm.email,
      name: editForm.name,
      locked: editForm.locked === "true",
      enabled: editForm.enabled === "true",
      reason: editForm.reason,
    });
    setEditUser(null);
    await load(pageState.page);
  };

  const runBulkAction = async () => {
    const targets = selectedUsers.filter((item) => item.username !== "admin" && !item.deleted);
    if (targets.length === 0 || !bulkAction) return;
    if (bulkAction === "lock") {
      await Promise.all(targets.map((item) => lockAdminUser(item.username)));
    }
    if (bulkAction === "disable") {
      await Promise.all(targets.map((item) => disableAdminUser(item.username)));
    }
    if (bulkAction === "delete") {
      await Promise.all(targets.map((item) => deleteAdminUser(item.username, "가입 사용자 선택 일괄 삭제")));
    }
    setBulkAction(null);
    setSelectedUsernames([]);
    await load(pageState.page);
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="외부 가입 사용자 관리"
      description="회원가입 또는 소셜 로그인으로 직접 생성된 계정을 조회하고 운영 상태를 관리합니다."
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
            name: "authMethod",
            label: "가입 방식",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "일반 회원가입", value: "PASSWORD" },
              { label: "소셜 전체", value: "SOCIAL" },
              { label: "Google", value: "GOOGLE" },
              { label: "Kakao", value: "KAKAO" },
              { label: "Naver", value: "NAVER" },
            ],
          },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0).catch(() => undefined)}
        onReset={() => void resetFilters().catch(() => undefined)}
      />

      <AdminBulkActionBar selectedLabel={`선택 ${selectedUsernames.length}건`}>
        <Button type="button" variant="outline" disabled={selectedUsernames.length === 0} onClick={() => setBulkAction("lock")}>선택 잠금</Button>
        <Button type="button" variant="outline" disabled={selectedUsernames.length === 0} onClick={() => setBulkAction("disable")}>선택 비활성화</Button>
        <Button type="button" variant="destructive" disabled={selectedUsernames.length === 0} onClick={() => setBulkAction("delete")}>선택 삭제</Button>
      </AdminBulkActionBar>

      <AdminTableCard>
        <table className={adminTableClassName}>
          <thead className={adminTheadClassName}>
            <tr>
              <th className={adminCellClassName}>
                <input type="checkbox" aria-label="현재 페이지 가입 사용자 전체 선택" checked={allPageSelected} onChange={(event) => togglePage(event.target.checked)} />
              </th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="사용자 ID" column="username" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>이름</th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="이메일" column="email" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>가입 방식</th>
              <th className={adminCellClassName}>상태</th>
              <th className={adminCellClassName}>MFA</th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="가입일" column="createdAt" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>작업</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? <AdminEmptyRow colSpan={9} /> : null}
            {users.map((item) => (
              <tr key={item.username} className={adminRowClassName}>
                <td className={adminCellClassName}>
                  <input
                    type="checkbox"
                    aria-label={`${item.username} 선택`}
                    disabled={item.username === "admin" || item.deleted}
                    checked={selectedUsernames.includes(item.username)}
                    onChange={(event) => toggleUser(item.username, event.target.checked)}
                  />
                </td>
                <td className={adminCellClassName}>
                  <Link className="font-medium text-primary hover:underline" to={`/admin/account/users/${item.username}`}>
                    {item.username}
                  </Link>
                  <div className="text-xs text-muted-foreground">{item.roles?.join(", ") || "ROLE_USER"}</div>
                </td>
                <td className={adminCellClassName}>{display(item.name ?? item.nickname)}</td>
                <td className={adminCellClassName}>{display(item.email)}</td>
                <td className={adminCellClassName}>
                  <AdminBadge tone={item.social ? "info" : "default"}>{authMethodLabel(item)}</AdminBadge>
                </td>
                <td className={adminCellClassName}>
                  <AdminBadge tone={statusTone(accountStatus(item))}>{accountStatus(item)}</AdminBadge>
                </td>
                <td className={adminCellClassName}>{item.mfaEnabled ? "ON" : "OFF"}</td>
                <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                  {item.createdAt ? formatSecurityDateTime(item.createdAt) : "-"}
                </td>
                <td className={adminCellClassName}>
                  <Button size="sm" variant="outline" onClick={() => openEdit(item)}>수정</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <AdminPagination pageState={pageState} onPageChange={(page) => void load(page).catch(() => undefined)} />
      </AdminTableCard>

      <AdminCrudModal
        open={editUser !== null}
        title="가입 사용자 수정"
        description="직접 가입한 사용자의 표시 정보와 계정 상태를 조정합니다."
        onOpenChange={(open) => {
          if (!open) setEditUser(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setEditUser(null)}>취소</Button>
            <Button type="button" onClick={() => void saveEdit().catch((error) => setFormError(error?.response?.data?.message ?? "저장에 실패했습니다."))}>저장</Button>
          </>
        }
      >
        {formError ? <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{formError}</p> : null}
        <AdminFormField label="이름" value={editForm.name} onChange={(value) => setEditForm((prev) => ({ ...prev, name: value }))} />
        <AdminFormField label="이메일" type="email" value={editForm.email} onChange={(value) => setEditForm((prev) => ({ ...prev, email: value }))} />
        <AdminSelectField
          label="잠금 상태"
          value={editForm.locked}
          options={[{ label: "정상", value: "false" }, { label: "잠금", value: "true" }]}
          onChange={(value) => setEditForm((prev) => ({ ...prev, locked: value }))}
        />
        <AdminSelectField
          label="활성 상태"
          value={editForm.enabled}
          options={[{ label: "활성", value: "true" }, { label: "비활성", value: "false" }]}
          onChange={(value) => setEditForm((prev) => ({ ...prev, enabled: value }))}
        />
        <AdminFormField label="사유" value={editForm.reason} onChange={(value) => setEditForm((prev) => ({ ...prev, reason: value }))} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={bulkAction !== null}
        title="가입 사용자 일괄 작업"
        description={`${selectedUsernames.length}개 계정에 ${bulkAction === "lock" ? "잠금" : bulkAction === "disable" ? "비활성화" : "삭제"} 작업을 실행합니다.`}
        confirmLabel={bulkAction === "delete" ? "삭제" : "실행"}
        destructive
        onOpenChange={(open) => {
          if (!open) setBulkAction(null);
        }}
        onConfirm={() => {
          void runBulkAction().catch(() => undefined);
        }}
      />
    </AdminPageShell>
  );
}
