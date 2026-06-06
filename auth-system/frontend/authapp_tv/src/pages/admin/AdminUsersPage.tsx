import { useEffect, useMemo, useState } from "react";
import { Link, Navigate } from "react-router";
import { Plus, SearchCheck } from "lucide-react";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminFilterOption, AdminFilterOptions, AdminUser, HrUserMaster } from "@/models/AdminModels";
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
  displayValue as display,
  statusTone,
} from "@/pages/admin/adminUi";
import {
  checkAdminUsernameExists,
  createAdminUser,
  deleteAdminUser,
  disableAdminUser,
  getAdminFilterOptions,
  getAdminUsers,
  getHrUserAccountCandidates,
  lockAdminUser,
  updateAdminUser,
} from "@/services/AdminService";

const blankUserForm = {
  employeeNo: "",
  username: "",
  password: "",
  roleName: "ROLE_USER",
  reason: "",
  email: "",
  name: "",
  locked: "false",
  enabled: "true",
};

const accountStatus = (item: AdminUser) => {
  if (item.deleted) return "DELETED";
  return item.status || "ACTIVE";
};

const passwordRules = [
  { label: "8자 이상", test: (value: string) => value.length >= 8 },
  { label: "영문 포함", test: (value: string) => /[A-Za-z]/.test(value) },
  { label: "숫자 포함", test: (value: string) => /\d/.test(value) },
  { label: "특수문자 포함", test: (value: string) => /[^A-Za-z0-9]/.test(value) },
];

export default function AdminUsersPage() {
  const user = useAuth((state) => state.user);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [candidates, setCandidates] = useState<HrUserMaster[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", role: "", mfaEnabled: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "createdAt", direction: "DESC" });
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [userForm, setUserForm] = useState(blankUserForm);
  const [usernameCheck, setUsernameCheck] = useState<"idle" | "checking" | "available" | "duplicate">("idle");
  const [formError, setFormError] = useState("");
  const [selectedUsernames, setSelectedUsernames] = useState<string[]>([]);
  const [bulkAction, setBulkAction] = useState<"lock" | "disable" | "delete" | null>(null);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  const roleOptions: AdminFilterOption[] = useMemo(
    () => [{ label: "초기역할 선택", value: "" }, ...(filterOptions?.roles ?? [])],
    [filterOptions],
  );

  const selectedCandidate = candidates.find((candidate) => candidate.employeeNo === userForm.employeeNo);
  const passwordValid = passwordRules.every((rule) => rule.test(userForm.password));

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminUsers({
      keyword: filters.keyword,
      status: filters.status,
      role: filters.role,
      mfaEnabled: filters.mfaEnabled,
      page: nextPage,
      size: pageState.size,
      sort: nextSort.sort,
      direction: nextSort.direction,
    });
    setUsers(page.content);
    setSelectedUsernames([]);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  useEffect(() => {
    if (isAdmin) {
      void getAdminFilterOptions().then(setFilterOptions).catch(() => undefined);
    }
  }, [isAdmin]);

  useEffect(() => {
    if (isAdmin) void load(0).catch(() => undefined);
  }, [isAdmin, filters.keyword, filters.status, filters.role, filters.mfaEnabled]);

  const openCreate = async () => {
    setSelectedUser(null);
    setUserForm(blankUserForm);
    setUsernameCheck("idle");
    setFormError("");
    setCandidates(await getHrUserAccountCandidates());
    setModalMode("create");
  };

  const openEdit = (item: AdminUser) => {
    setSelectedUser(item);
                          setUserForm({
                            ...blankUserForm,
                            username: item.username,
                            email: item.email || "",
                            name: item.name || item.nickname || "",
                            locked: String(item.locked),
                            enabled: String(item.enabled),
                          });
    setFormError("");
    setModalMode("edit");
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
    setFilters({ keyword: "", status: "", role: "", mfaEnabled: "" });
    const page = await getAdminUsers({ page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
    setUsers(page.content);
    setSelectedUsernames([]);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const checkUsername = async () => {
    const username = userForm.username.trim();
    if (!username) {
      setFormError("사용자 ID를 입력한 뒤 중복확인하세요.");
      return;
    }
    setUsernameCheck("checking");
    const result = await checkAdminUsernameExists(username);
    setUsernameCheck(result.exists ? "duplicate" : "available");
  };

  const validateCreate = () => {
    if (!userForm.employeeNo) return "HR 직원을 선택하세요.";
    if (!userForm.username.trim()) return "사용자 ID를 입력하세요.";
    if (usernameCheck !== "available") return "사용자 ID 중복확인을 완료하세요.";
    if (!passwordValid) return "초기 비밀번호 규칙을 확인하세요.";
    if (!userForm.roleName) return "초기역할을 선택하세요.";
    return "";
  };

  const save = async () => {
    if (modalMode === "create") {
      const message = validateCreate();
      if (message) {
        setFormError(message);
        return;
      }
      await createAdminUser({
        employeeNo: userForm.employeeNo,
        username: userForm.username.trim(),
        password: userForm.password,
        roleName: userForm.roleName,
        reason: userForm.reason,
      });
    } else if (selectedUser) {
      await updateAdminUser(selectedUser.username, {
        email: userForm.email,
        name: userForm.name,
        locked: userForm.locked === "true",
        enabled: userForm.enabled === "true",
        reason: userForm.reason,
      });
    }
    setModalMode(null);
    await load(0);
  };

  const selectableUsers = users.filter((item) => item.username !== "admin" && !item.deleted);
  const selectedUsers = users.filter((item) => selectedUsernames.includes(item.username));
  const allPageSelected = selectableUsers.length > 0 && selectableUsers.every((item) => selectedUsernames.includes(item.username));

  const toggleUser = (username: string, checked: boolean) => {
    setSelectedUsernames((current) => checked ? Array.from(new Set([...current, username])) : current.filter((item) => item !== username));
  };

  const togglePage = (checked: boolean) => {
    setSelectedUsernames(checked ? selectableUsers.map((item) => item.username) : []);
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
      await Promise.all(targets.map((item) => deleteAdminUser(item.username, "관리자 선택 일괄 삭제")));
    }
    setBulkAction(null);
    setSelectedUsernames([]);
    await load();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="사용자 관리"
      description="HR 기준정보에 등록된 직원을 선택해 인증 계정을 생성하고 계정 상태를 운영합니다."
      actions={<Button type="button" onClick={() => void openCreate().catch(() => undefined)}><Plus className="h-4 w-4" />계정 생성</Button>}
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
                  <input type="checkbox" aria-label="현재 페이지 사용자 전체 선택" checked={allPageSelected} onChange={(event) => togglePage(event.target.checked)} />
                </th>
                <th className={adminCellClassName}><AdminSortableHeader label="사용자 ID" column="username" sortState={sortState} onSort={handleSort} /></th>
                <th className={adminCellClassName}>이름</th>
                <th className={adminCellClassName}>이메일</th>
                <th className={adminCellClassName}>상태</th>
                <th className={adminCellClassName}>유형</th>
                <th className={adminCellClassName}>인증</th>
                <th className={adminCellClassName}>MFA</th>
                <th className={adminCellClassName}>마지막 로그인</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? <AdminEmptyRow colSpan={10} /> : null}
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
                  <td className={adminCellClassName}><AdminBadge tone={statusTone(accountStatus(item))}>{accountStatus(item)}</AdminBadge></td>
                  <td className={adminCellClassName}>{item.userType || (item.social ? "SOCIAL" : "INTERNAL")}</td>
                  <td className={adminCellClassName}>{item.authMethod || "PASSWORD"}</td>
                  <td className={adminCellClassName}>{item.mfaEnabled ? "ON" : "OFF"}</td>
                  <td className={adminCellClassName}>{item.lastLoginAt ? formatSecurityDateTime(item.lastLoginAt) : "-"}</td>
                  <td className={adminCellClassName}>
                    <div className="flex flex-wrap justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openEdit(item)}>수정</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={pageState} onPageChange={(page) => void load(page).catch(() => undefined)} />
      </AdminTableCard>

      <AdminCrudModal
        open={modalMode !== null}
        title={modalMode === "create" ? "HR 직원 계정 생성" : "사용자 수정"}
        description={modalMode === "create" ? "HR 기준정보에 등록된 계정 미생성 직원을 선택합니다." : "인증 계정의 표시 이름과 이메일을 수정합니다."}
        onOpenChange={(open) => {
          if (!open) setModalMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalMode(null)}>취소</Button>
            <Button type="button" onClick={() => void save().catch((error) => setFormError(error?.response?.data?.message ?? "저장에 실패했습니다."))}>저장</Button>
          </>
        }
      >
        {formError ? <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{formError}</p> : null}
        {modalMode === "create" ? (
          <>
            <AdminSelectField
              label="HR 직원"
              value={userForm.employeeNo}
              options={[
                { label: "직원 선택", value: "" },
                ...candidates.map((candidate) => ({
                  label: `${candidate.employeeNo} / ${candidate.name} / ${candidate.departmentName || "-"}`,
                  value: candidate.employeeNo,
                })),
              ]}
              onChange={(value) => setUserForm((prev) => ({ ...prev, employeeNo: value }))}
            />
            {selectedCandidate ? (
              <div className="grid gap-2 rounded-lg border p-3 text-sm sm:grid-cols-2">
                <AdminInfoItem label="이름" value={selectedCandidate.name} />
                <AdminInfoItem label="이메일" value={selectedCandidate.email} />
                <AdminInfoItem label="부서" value={display(selectedCandidate.departmentName)} />
                <AdminInfoItem label="직책" value={display(selectedCandidate.position)} />
              </div>
            ) : null}
            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">사용자 ID</Label>
              <div className="flex gap-2">
                <Input
                  value={userForm.username}
                  onChange={(event) => {
                    setUserForm((prev) => ({ ...prev, username: event.target.value }));
                    setUsernameCheck("idle");
                  }}
                />
                <Button type="button" variant="outline" disabled={usernameCheck === "checking"} onClick={() => void checkUsername().catch(() => setFormError("중복확인에 실패했습니다."))}>
                  <SearchCheck className="h-4 w-4" />확인
                </Button>
              </div>
              <p className={usernameCheck === "duplicate" ? "text-xs text-destructive" : "text-xs text-muted-foreground"}>
                {usernameCheck === "available" ? "사용 가능" : usernameCheck === "duplicate" ? "이미 사용 중" : usernameCheck === "checking" ? "확인 중" : "중복확인 필요"}
              </p>
            </div>
            <AdminFormField label="초기 비밀번호" type="password" value={userForm.password} onChange={(value) => setUserForm((prev) => ({ ...prev, password: value }))} />
            <div className="grid gap-1 text-xs text-muted-foreground sm:grid-cols-2">
              {passwordRules.map((rule) => (
                <span key={rule.label} className={rule.test(userForm.password) ? "text-emerald-600" : ""}>{rule.test(userForm.password) ? "✓" : "•"} {rule.label}</span>
              ))}
            </div>
            <AdminSelectField
              label="초기역할"
              value={userForm.roleName}
              options={roleOptions}
              onChange={(value) => setUserForm((prev) => ({ ...prev, roleName: value }))}
            />
          </>
        ) : (
          <>
            <AdminFormField label="이름" value={userForm.name} onChange={(value) => setUserForm((prev) => ({ ...prev, name: value }))} />
            <AdminFormField label="이메일" type="email" value={userForm.email} onChange={(value) => setUserForm((prev) => ({ ...prev, email: value }))} />
            <AdminSelectField
              label="잠금 상태"
              value={userForm.locked}
              options={[{ label: "정상", value: "false" }, { label: "잠금", value: "true" }]}
              onChange={(value) => setUserForm((prev) => ({ ...prev, locked: value }))}
            />
            <AdminSelectField
              label="활성 상태"
              value={userForm.enabled}
              options={[{ label: "활성", value: "true" }, { label: "비활성", value: "false" }]}
              onChange={(value) => setUserForm((prev) => ({ ...prev, enabled: value }))}
            />
          </>
        )}
        <AdminFormField label="사유" value={userForm.reason} onChange={(value) => setUserForm((prev) => ({ ...prev, reason: value }))} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={bulkAction !== null}
        title="선택 사용자 일괄 작업"
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
