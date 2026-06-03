import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminApiPermissionRule, AdminApiPermissionRuleRequest, AdminPermission } from "@/models/AdminModels";
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
  createAdminApiPermissionRule,
  deleteAdminApiPermissionRule,
  getAdminApiPermissionRules,
  getAdminPermissions,
  updateAdminApiPermissionRule,
} from "@/services/AdminService";

const blankRuleForm = {
  httpMethod: "GET",
  pathPattern: "",
  permissionCode: "",
  description: "",
  enabled: "true",
  sortOrder: "100",
  reason: "",
};

const methodOptions = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "*"].map((method) => ({
  label: method,
  value: method,
}));

const statusLabel = (enabled: boolean) => (enabled ? "사용" : "비활성");
const contains = (value: string | number | boolean | null | undefined, keyword: string) =>
  String(value ?? "").toLowerCase().includes(keyword.trim().toLowerCase());
const compareText = (left?: string | null, right?: string | null) => String(left ?? "").localeCompare(String(right ?? ""));
const display = (value?: string | number | null) => (value === null || value === undefined || value === "" ? "-" : String(value));

export default function AdminPermissionManagementPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [rules, setRules] = useState<AdminApiPermissionRule[]>([]);
  const [permissions, setPermissions] = useState<AdminPermission[]>([]);
  const [filters, setFilters] = useState({ keyword: "", method: "", permissionCode: "", status: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "sortOrder", direction: "ASC" });
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editRule, setEditRule] = useState<AdminApiPermissionRule | null>(null);
  const [ruleForm, setRuleForm] = useState(blankRuleForm);
  const [deleteTarget, setDeleteTarget] = useState<AdminApiPermissionRule | null>(null);

  const load = async () => {
    const [nextRules, nextPermissions] = await Promise.all([getAdminApiPermissionRules(), getAdminPermissions()]);
    setRules(nextRules);
    setPermissions(nextPermissions);
    setEditRule((current) => (current ? nextRules.find((rule) => rule.id === current.id) ?? current : current));
  };

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin]);

  const permissionOptions = useMemo(
    () => [
      { label: "권한 선택", value: "" },
      ...permissions
        .filter((permission) => permission.enabled)
        .map((permission) => ({ label: `${permission.code} - ${permission.name}`, value: permission.code })),
    ],
    [permissions],
  );

  const filterPermissionOptions = useMemo(
    () => [{ label: "전체", value: "" }, ...permissions.map((permission) => ({ label: permission.code, value: permission.code }))],
    [permissions],
  );

  const filteredRules = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = rules.filter((rule) => {
      const status = rule.enabled ? "ACTIVE" : "DISABLED";
      return (!keyword || [rule.httpMethod, rule.pathPattern, rule.permissionCode, rule.description].some((value) => contains(value, keyword)))
        && (!filters.method || rule.httpMethod === filters.method)
        && (!filters.permissionCode || rule.permissionCode === filters.permissionCode)
        && (!filters.status || status === filters.status);
    });

    return [...filtered].sort((left, right) => {
      const direction = sortState.direction === "ASC" ? 1 : -1;
      const result = (() => {
        switch (sortState.sort) {
          case "httpMethod":
            return compareText(left.httpMethod, right.httpMethod);
          case "pathPattern":
            return compareText(left.pathPattern, right.pathPattern);
          case "permissionCode":
            return compareText(left.permissionCode, right.permissionCode);
          case "enabled":
            return Number(left.enabled) - Number(right.enabled);
          case "sortOrder":
          default:
            return left.sortOrder - right.sortOrder;
        }
      })();
      return result * direction;
    });
  }, [rules, filters, sortState]);

  const listPageState = {
    ...pageState,
    totalElements: filteredRules.length,
    totalPages: Math.max(Math.ceil(filteredRules.length / pageState.size), 1),
  };
  const pagedRules = filteredRules.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size);

  const handleSort = (column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
    setPageState((current) => ({ ...current, page: 0 }));
  };

  const openRuleModal = (mode: "create" | "edit", rule?: AdminApiPermissionRule) => {
    setModalMode(mode);
    setEditRule(rule ?? null);
    setRuleForm(
      rule
        ? {
            httpMethod: rule.httpMethod,
            pathPattern: rule.pathPattern,
            permissionCode: rule.permissionCode,
            description: rule.description || "",
            enabled: String(rule.enabled),
            sortOrder: String(rule.sortOrder),
            reason: "",
          }
        : blankRuleForm,
    );
  };

  const saveRule = async () => {
    const payload: AdminApiPermissionRuleRequest = {
      httpMethod: ruleForm.httpMethod,
      pathPattern: ruleForm.pathPattern.trim(),
      permissionCode: ruleForm.permissionCode.trim(),
      description: ruleForm.description,
      enabled: ruleForm.enabled === "true",
      sortOrder: Number(ruleForm.sortOrder || 100),
      reason: ruleForm.reason || (modalMode === "create" ? "API 권한 규칙 생성" : "API 권한 규칙 수정"),
    };

    if (modalMode === "edit" && editRule) {
      await updateAdminApiPermissionRule(editRule.id, payload);
    } else {
      await createAdminApiPermissionRule(payload);
    }

    setModalMode(null);
    setRuleForm(blankRuleForm);
    await load();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="관리자 권한 관리"
      description="API 요청 방식과 경로 패턴을 권한 코드에 연결해 데이터베이스 기반 접근 제어를 관리합니다."
      actions={<Button type="button" onClick={() => openRuleModal("create")}>규칙 추가</Button>}
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "검색", placeholder: "방식, 경로, 권한, 설명" },
          { name: "method", label: "요청 방식", type: "select", options: [{ label: "전체", value: "" }, ...methodOptions] },
          { name: "permissionCode", label: "권한", type: "select", options: filterPermissionOptions },
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
        ]}
        values={filters}
        onChange={(name, value) => {
          setFilters((current) => ({ ...current, [name]: value }));
          setPageState((current) => ({ ...current, page: 0 }));
        }}
        onSubmit={() => setPageState((current) => ({ ...current, page: 0 }))}
        onReset={() => {
          setFilters({ keyword: "", method: "", permissionCode: "", status: "" });
          setPageState((current) => ({ ...current, page: 0 }));
        }}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="정렬 순서" column="sortOrder" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="요청 방식" column="httpMethod" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="경로 패턴" column="pathPattern" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="권한" column="permissionCode" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>설명</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="enabled" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>수정일</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {pagedRules.length === 0 ? <AdminEmptyRow colSpan={8} message="등록된 API 권한 규칙이 없습니다." /> : null}
              {pagedRules.map((rule) => (
                <tr key={rule.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>{rule.sortOrder}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone="info">{rule.httpMethod}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <code className="rounded bg-muted px-2 py-1 text-xs">{rule.pathPattern}</code>
                  </td>
                  <td className={adminCellClassName}>{rule.permissionCode}</td>
                  <td className={adminCellClassName}>{display(rule.description)}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(rule.enabled)}>{statusLabel(rule.enabled)}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{rule.updatedAt ? formatSecurityDateTime(rule.updatedAt) : "-"}</td>
                  <td className={adminCellClassName}>
                    <div className="flex justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openRuleModal("edit", rule)}>수정</Button>
                      <Button size="sm" variant="destructive" onClick={() => setDeleteTarget(rule)}>삭제</Button>
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
        title={modalMode === "create" ? "API 권한 규칙 추가" : "API 권한 규칙 수정"}
        description="활성화된 규칙과 일치하는 요청은 지정된 권한 코드가 있어야 통과합니다."
        onOpenChange={(open) => {
          if (!open) setModalMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setModalMode(null)}>취소</Button>
            <Button type="button" onClick={() => void saveRule()}>저장</Button>
          </>
        }
      >
        <SelectField
          label="HTTP 요청 방식"
          value={ruleForm.httpMethod}
          options={methodOptions}
          onChange={(value) => setRuleForm((current) => ({ ...current, httpMethod: value }))}
        />
        <Field
          label="경로 패턴"
          value={ruleForm.pathPattern}
          placeholder="/api/v1/admin/users/{username}"
          onChange={(value) => setRuleForm((current) => ({ ...current, pathPattern: value }))}
        />
        <SelectField
          label="권한 코드"
          value={ruleForm.permissionCode}
          options={permissionOptions}
          onChange={(value) => setRuleForm((current) => ({ ...current, permissionCode: value }))}
        />
        <Field label="설명" value={ruleForm.description} onChange={(value) => setRuleForm((current) => ({ ...current, description: value }))} />
        <SelectField
          label="상태"
          value={ruleForm.enabled}
          options={[{ label: "사용", value: "true" }, { label: "비활성", value: "false" }]}
          onChange={(value) => setRuleForm((current) => ({ ...current, enabled: value }))}
        />
        <Field label="정렬 순서" type="number" value={ruleForm.sortOrder} onChange={(value) => setRuleForm((current) => ({ ...current, sortOrder: value }))} />
        <Field label="사유" value={ruleForm.reason} onChange={(value) => setRuleForm((current) => ({ ...current, reason: value }))} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={deleteTarget !== null}
        title="API 권한 규칙 삭제"
        description={`${deleteTarget?.httpMethod ?? ""} ${deleteTarget?.pathPattern ?? ""} 규칙을 삭제합니다. 다른 규칙이 같은 요청을 처리하지 않으면 RBAC가 해당 요청을 거부할 수 있습니다.`}
        confirmLabel="삭제"
        destructive
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        onConfirm={() => {
          if (!deleteTarget) return;
          void deleteAdminApiPermissionRule(deleteTarget.id).then(load);
        }}
      />
    </AdminPageShell>
  );
}

function Field({
  label,
  value,
  onChange,
  disabled = false,
  placeholder,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
  type?: string;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <Input type={type} value={value} disabled={disabled} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} />
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
