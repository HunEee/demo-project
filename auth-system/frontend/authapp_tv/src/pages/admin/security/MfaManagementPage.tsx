import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { Button } from "@/components/ui/button";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBulkActionBar,
  compareText,
  containsText,
  type SortState,
} from "@/pages/admin/adminUi";
import MfaActionDialogs from "@/pages/admin/security/mfa/MfaActionDialogs";
import MfaPolicyToolbar from "@/pages/admin/security/mfa/MfaPolicyToolbar";
import MfaUserTable from "@/pages/admin/security/mfa/MfaUserTable";
import { useAdminClientList, useBulkSelection } from "@/pages/admin/useAdminList";
import type { AdminMfaUserResponse, MfaPolicy } from "@/models/MfaModels";
import {
  createAdminMfaException,
  getAdminMfaPolicy,
  getAdminMfaUsers,
  resetAdminUserMfa,
  revokeAdminMfaException,
  updateAdminMfaPolicy,
} from "@/services/MfaService";

type Filters = {
  keyword: string;
  mfaEnabled: string;
  exceptionActive: string;
  requiredByPolicy: string;
};

type BulkAction = "reset" | "exception" | null;

const initialFilters: Filters = {
  keyword: "",
  mfaEnabled: "",
  exceptionActive: "",
  requiredByPolicy: "",
};

const initialSort: SortState = {
  sort: "username",
  direction: "ASC",
};

const sortUsers = (left: AdminMfaUserResponse, right: AdminMfaUserResponse, sortState: SortState) => {
  const direction = sortState.direction === "ASC" ? 1 : -1;
  const result =
    sortState.sort === "mfaEnabled"
      ? Number(left.mfaEnabled) - Number(right.mfaEnabled)
      : sortState.sort === "lastUsedAt"
        ? compareText(left.lastUsedAt, right.lastUsedAt)
        : sortState.sort === "exceptionExpiresAt"
          ? compareText(left.exceptionExpiresAt, right.exceptionExpiresAt)
          : compareText(left.username, right.username);

  return result * direction;
};

export default function MfaManagementPage() {
  const [users, setUsers] = useState<AdminMfaUserResponse[]>([]);
  const [policy, setPolicy] = useState<MfaPolicy>("OPTIONAL");
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [loading, setLoading] = useState(true);
  const [resetTarget, setResetTarget] = useState<AdminMfaUserResponse | null>(null);
  const [exceptionTarget, setExceptionTarget] = useState<AdminMfaUserResponse | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<AdminMfaUserResponse | null>(null);
  const [bulkAction, setBulkAction] = useState<BulkAction>(null);
  const [resetReason, setResetReason] = useState("관리자 MFA 초기화");
  const [exceptionReason, setExceptionReason] = useState("임시 MFA 예외");
  const [exceptionExpiresAt, setExceptionExpiresAt] = useState("");
  const [formError, setFormError] = useState("");

  const load = async () => {
    setLoading(true);
    const [userData, policyData] = await Promise.all([getAdminMfaUsers(), getAdminMfaPolicy()]);
    setUsers(userData);
    setPolicy(policyData.policy);
    setLoading(false);
  };

  useEffect(() => {
    void load().catch((error) => {
      console.error(error);
      toast.error("MFA 관리 정보를 불러오지 못했습니다.");
      setLoading(false);
    });
  }, []);

  const filter = useMemo(
    () => (user: AdminMfaUserResponse) =>
      (containsText(user.username, filters.keyword) || containsText(user.email, filters.keyword)) &&
      (!filters.mfaEnabled || String(user.mfaEnabled) === filters.mfaEnabled) &&
      (!filters.exceptionActive || String(user.exceptionActive) === filters.exceptionActive) &&
      (!filters.requiredByPolicy || String(user.requiredByPolicy) === filters.requiredByPolicy),
    [filters],
  );

  const { pagedItems, listPageState, sortState, handleSort, resetPage, setPage } = useAdminClientList({
    items: users,
    filter,
    sort: sortUsers,
    initialSort,
    pageSize: 10,
  });

  const selection = useBulkSelection(() => pagedItems.map((user) => user.username));
  const selectedUsers = users.filter((user) => selection.selectedIds.includes(user.username));

  const handleFilterChange = (name: string, value: string) => {
    setFilters((current) => ({ ...current, [name]: value }));
    selection.clearSelection();
    resetPage();
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    selection.clearSelection();
    resetPage();
  };

  const handlePolicySave = async () => {
    try {
      const response = await updateAdminMfaPolicy(policy);
      setPolicy(response.policy);
      toast.success("MFA 정책이 저장되었습니다.");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "정책 저장에 실패했습니다.");
    }
  };

  const runReset = async (targets: AdminMfaUserResponse[], reason: string) => {
    const resettable = targets.filter((user) => user.mfaEnabled);
    if (resettable.length === 0) {
      toast.error("초기화할 MFA 등록 계정이 없습니다.");
      return;
    }
    await Promise.all(resettable.map((user) => resetAdminUserMfa(user.username, reason)));
    toast.success(`${resettable.length}개 계정의 MFA가 초기화되었습니다.`);
    selection.clearSelection();
    await load();
  };

  const runException = async (targets: AdminMfaUserResponse[], reason: string, expiresAt: string) => {
    if (!expiresAt) {
      setFormError("예외 만료일시를 입력하세요.");
      return;
    }
    await Promise.all(targets.map((user) => createAdminMfaException(user.username, reason, expiresAt)));
    toast.success(`${targets.length}개 계정에 MFA 예외가 등록되었습니다.`);
    selection.clearSelection();
    await load();
  };

  const confirmReset = async () => {
    const targets = resetTarget ? [resetTarget] : selectedUsers;
    await runReset(targets, resetReason);
    setResetTarget(null);
    setBulkAction(null);
  };

  const confirmException = async () => {
    const targets = exceptionTarget ? [exceptionTarget] : selectedUsers;
    await runException(targets, exceptionReason, exceptionExpiresAt);
    setExceptionTarget(null);
    setBulkAction(null);
  };

  const confirmRevoke = async () => {
    if (!revokeTarget) return;
    await revokeAdminMfaException(revokeTarget.username);
    toast.success(`${revokeTarget.username} MFA 예외가 해제되었습니다.`);
    setRevokeTarget(null);
    await load();
  };

  const openBulkReset = () => {
    setResetReason("관리자 MFA 일괄 초기화");
    setBulkAction("reset");
  };

  const openBulkException = () => {
    setExceptionReason("임시 MFA 일괄 예외");
    setExceptionExpiresAt("");
    setFormError("");
    setBulkAction("exception");
  };

  return (
    <AdminPageShell
      title="MFA 관리"
      description="사용자는 직접 TOTP를 등록해 MFA 사용을 선택하고, 관리자는 상태 확인, 초기화, 임시 예외와 미등록 차단 정책을 관리합니다."
      actions={<MfaPolicyToolbar policy={policy} onPolicyChange={setPolicy} onSave={handlePolicySave} />}
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "사용자 검색", placeholder: "아이디 또는 이메일" },
          {
            name: "mfaEnabled",
            label: "MFA 등록",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "등록", value: "true" },
              { label: "미등록", value: "false" },
            ],
          },
          {
            name: "exceptionActive",
            label: "예외",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "예외 있음", value: "true" },
              { label: "예외 없음", value: "false" },
            ],
          },
          {
            name: "requiredByPolicy",
            label: "정책 대상",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "대상", value: "true" },
              { label: "비대상", value: "false" },
            ],
          },
        ]}
        values={filters}
        onChange={handleFilterChange}
        onSubmit={resetPage}
        onReset={handleResetFilters}
        hint="클라이언트 필터 적용"
      />

      <AdminBulkActionBar selectedLabel={`선택 ${selection.selectedCount}건`}>
        <Button type="button" variant="outline" disabled={selection.selectedCount === 0} onClick={openBulkReset}>
          선택 초기화
        </Button>
        <Button type="button" variant="outline" disabled={selection.selectedCount === 0} onClick={openBulkException}>
          선택 예외
        </Button>
      </AdminBulkActionBar>

      <MfaUserTable
        loading={loading}
        users={pagedItems}
        sortState={sortState}
        onSort={handleSort}
        selection={selection}
        pageState={listPageState}
        onPageChange={setPage}
        onReset={(user) => {
          setResetTarget(user);
          setResetReason("관리자 MFA 초기화");
        }}
        onException={(user) => {
          setExceptionTarget(user);
          setExceptionReason("임시 MFA 예외");
          setExceptionExpiresAt("");
          setFormError("");
        }}
        onRevokeException={setRevokeTarget}
      />

      <MfaActionDialogs
        resetOpen={resetTarget !== null || bulkAction === "reset"}
        exceptionOpen={exceptionTarget !== null || bulkAction === "exception"}
        resetTarget={resetTarget}
        exceptionTarget={exceptionTarget}
        revokeTarget={revokeTarget}
        selectedCount={selectedUsers.length}
        resetReason={resetReason}
        exceptionReason={exceptionReason}
        exceptionExpiresAt={exceptionExpiresAt}
        formError={formError}
        onResetReasonChange={setResetReason}
        onExceptionReasonChange={setExceptionReason}
        onExceptionExpiresAtChange={setExceptionExpiresAt}
        onCloseReset={() => {
          setResetTarget(null);
          setBulkAction(null);
        }}
        onCloseException={() => {
          setExceptionTarget(null);
          setBulkAction(null);
        }}
        onCloseRevoke={() => setRevokeTarget(null)}
        onConfirmReset={confirmReset}
        onConfirmException={confirmException}
        onConfirmRevoke={confirmRevoke}
      />
    </AdminPageShell>
  );
}
