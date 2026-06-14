import { useEffect, useState } from "react";
import { LockKeyhole, RotateCcw, ShieldAlert } from "lucide-react";
import { Navigate } from "react-router";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminFilterOptions, AdminRisk, AdminRiskEvent } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
  AdminCrudModal,
  AdminEmptyRow,
  AdminFormField,
  AdminPagination,
  AdminSortableHeader,
  AdminTableCard,
  type PageState,
  type SortState,
  adminCellClassName,
  adminRowClassName,
  adminTableClassName,
  adminTheadClassName,
  statusTone,
} from "@/pages/admin/adminUi";
import {
  getAdminFilterOptions,
  getAdminRiskEvents,
  getAdminRisks,
  lockRiskUser,
  requireRiskUserMfa,
  resolveAdminRiskEvent,
  revokeRiskUserTokens,
} from "@/services/AdminService";

type RiskAction = "lock" | "revoke" | "mfa";

const actionMeta: Record<RiskAction, { title: string; label: string; defaultReason: string }> = {
  lock: {
    title: "위험 계정 잠금",
    label: "계정 잠금",
    defaultReason: "HIGH 이상 위험 사용자 수동 계정 잠금",
  },
  revoke: {
    title: "토큰 폐기",
    label: "토큰 폐기",
    defaultReason: "HIGH 이상 위험 사용자 토큰 폐기",
  },
  mfa: {
    title: "MFA 재등록 요구",
    label: "MFA 요구",
    defaultReason: "HIGH 이상 위험 사용자 MFA 재등록 요구",
  },
};

const riskLevelLabel = (value?: string) => {
  switch (String(value ?? "").toUpperCase()) {
    case "LOW":
      return "낮음";
    case "MEDIUM":
      return "보통";
    case "HIGH":
      return "높음";
    case "CRITICAL":
      return "치명";
    default:
      return value || "-";
  }
};

export default function AdminRiskPage() {
  const user = useAuth((state) => state.user);
  const [items, setItems] = useState<AdminRisk[]>([]);
  const [riskEvents, setRiskEvents] = useState<AdminRiskEvent[]>([]);
  const [filters, setFilters] = useState({ username: "", level: "", minScore: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [riskEventPageState, setRiskEventPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "riskScore", direction: "DESC" });
  const [pendingAction, setPendingAction] = useState<{ item: AdminRisk; action: RiskAction } | null>(null);
  const [reason, setReason] = useState("");
  const isAdmin = hasAdminAccess(user);

  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminRisks({
      username: filters.username,
      level: filters.level,
      minScore: filters.minScore,
      page: nextPage,
      size: pageState.size,
      sort: nextSort.sort,
      direction: nextSort.direction,
    });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const loadRiskEvents = async (nextPage = riskEventPageState.page) => {
    const page = await getAdminRiskEvents({
      page: nextPage,
      size: riskEventPageState.size,
      sort: "createdAt",
      direction: "DESC",
    });
    setRiskEvents(page.content);
    setRiskEventPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
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
    const nextFilters = { username: "", level: "", minScore: "" };
    setFilters(nextFilters);
    const page = await getAdminRisks({
      username: nextFilters.username,
      level: nextFilters.level,
      minScore: nextFilters.minScore,
      page: 0,
      size: pageState.size,
      sort: sortState.sort,
      direction: sortState.direction,
    });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const canAct = (level?: string) => ["HIGH", "CRITICAL"].includes(String(level ?? "").toUpperCase());

  const openAction = (item: AdminRisk, action: RiskAction) => {
    setPendingAction({ item, action });
    setReason(actionMeta[action].defaultReason);
  };

  const submitAction = async () => {
    if (!pendingAction || reason.trim() === "") return;
    const { item, action } = pendingAction;
    if (action === "lock") await lockRiskUser(item.username, reason);
    if (action === "revoke") await revokeRiskUserTokens(item.username, reason);
    if (action === "mfa") await requireRiskUserMfa(item.username, reason);
    setPendingAction(null);
    setReason("");
    await load();
  };

  const resolveRiskEvent = async (id: number) => {
    await resolveAdminRiskEvent(id);
    await loadRiskEvents();
  };

  useEffect(() => {
    if (isAdmin) {
      void load().catch(() => undefined);
      void loadRiskEvents().catch(() => undefined);
      void getAdminFilterOptions().then(setFilterOptions).catch(() => undefined);
    }
    // Initial admin load only; filters, pagination, and actions call loaders explicitly.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="위험 사용자 관리"
      description="위험 점수와 최근 판단 사유를 확인하고 HIGH 이상 사용자에게 수동 대응을 실행합니다."
    >
      <AdminFilters
        fields={[
          { name: "username", label: "사용자 검색", placeholder: "아이디" },
          {
            name: "level",
            label: "위험도",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.riskLevels ?? [])],
          },
          { name: "minScore", label: "최소 점수", type: "number", placeholder: "예: 60" },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <AdminTableCard>
        <table className={`${adminTableClassName} min-w-[980px]`}>
          <thead className={adminTheadClassName}>
            <tr>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="점수" column="riskScore" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="위험도" column="riskLevel" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="최근 사유" column="lastReason" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>
                <AdminSortableHeader label="갱신 시각" column="updatedAt" sortState={sortState} onSort={handleSort} />
              </th>
              <th className={adminCellClassName}>수동 대응</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? <AdminEmptyRow colSpan={6} /> : null}
            {items.map((item) => (
              <tr key={item.id} className={adminRowClassName}>
                <td className={adminCellClassName}>{item.username}</td>
                <td className={`${adminCellClassName} tabular-nums`}>{item.riskScore}</td>
                <td className={adminCellClassName}>
                  <AdminBadge tone={statusTone(item.riskLevel)}>{riskLevelLabel(item.riskLevel)}</AdminBadge>
                </td>
                <td className={`${adminCellClassName} max-w-72 text-left`}>{item.lastReason || "-"}</td>
                <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                  {formatSecurityDateTime(item.updatedAt)}
                </td>
                <td className={adminCellClassName}>
                  <div className="flex min-w-[220px] flex-wrap justify-center gap-1.5">
                    <Button size="sm" variant="outline" disabled={!canAct(item.riskLevel)} onClick={() => openAction(item, "lock")}>
                      <LockKeyhole className="h-4 w-4" />
                      잠금
                    </Button>
                    <Button size="sm" variant="outline" disabled={!canAct(item.riskLevel)} onClick={() => openAction(item, "revoke")}>
                      <RotateCcw className="h-4 w-4" />
                      토큰
                    </Button>
                    <Button size="sm" variant="outline" disabled={!canAct(item.riskLevel)} onClick={() => openAction(item, "mfa")}>
                      <ShieldAlert className="h-4 w-4" />
                      MFA
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
      </AdminTableCard>

      <section className="grid gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-normal">위험 로그인 이벤트</h2>
          <p className="mt-1 text-sm text-muted-foreground">룰 기반으로 자동 생성된 위험 로그인 이벤트입니다.</p>
        </div>
        <AdminTableCard>
          <table className={`${adminTableClassName} min-w-[1080px]`}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>시간</th>
                <th className={adminCellClassName}>사용자</th>
                <th className={adminCellClassName}>이벤트</th>
                <th className={adminCellClassName}>위험도</th>
                <th className={adminCellClassName}>점수</th>
                <th className={adminCellClassName}>IP</th>
                <th className={adminCellClassName}>User-Agent</th>
                <th className={adminCellClassName}>상태</th>
                <th className={adminCellClassName}>처리</th>
              </tr>
            </thead>
            <tbody>
              {riskEvents.length === 0 ? <AdminEmptyRow colSpan={9} /> : null}
              {riskEvents.map((event) => (
                <tr key={event.id} className={adminRowClassName}>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(event.createdAt)}
                  </td>
                  <td className={adminCellClassName}>{event.username}</td>
                  <td className={adminCellClassName}>{event.eventType || "-"}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(event.riskLevel)}>{riskLevelLabel(event.riskLevel)}</AdminBadge>
                  </td>
                  <td className={`${adminCellClassName} tabular-nums`}>{event.score}</td>
                  <td className={adminCellClassName}>{event.ipAddress || "-"}</td>
                  <td className={`${adminCellClassName} max-w-64 break-all text-xs`}>{event.userAgent || event.device || "-"}</td>
                  <td className={adminCellClassName}>{event.resolved ? "해결" : "미해결"}</td>
                  <td className={adminCellClassName}>
                    <Button size="sm" variant="outline" disabled={event.resolved} onClick={() => void resolveRiskEvent(event.id)}>
                      해결
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={riskEventPageState} onPageChange={(page) => void loadRiskEvents(page)} />
        </AdminTableCard>
      </section>

      <AdminCrudModal
        open={pendingAction !== null}
        title={pendingAction ? actionMeta[pendingAction.action].title : "위험 대응"}
        description={pendingAction ? `${pendingAction.item.username} 사용자에게 수동 대응을 실행합니다.` : undefined}
        onOpenChange={(open) => {
          if (!open) setPendingAction(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setPendingAction(null)}>
              취소
            </Button>
            <Button type="button" disabled={reason.trim() === ""} onClick={() => void submitAction()}>
              실행
            </Button>
          </>
        }
      >
        <AdminFormField label="대응 사유" value={reason} onChange={setReason} placeholder="감사 로그에 남길 사유" />
      </AdminCrudModal>
    </AdminPageShell>
  );
}
