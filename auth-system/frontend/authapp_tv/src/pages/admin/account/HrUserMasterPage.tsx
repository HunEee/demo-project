import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import { Plus, SearchCheck } from "lucide-react";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { HrUserMaster, HrUserMasterRequest } from "@/models/AdminModels";
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
  containsText as contains,
  displayValue as display,
  statusTone,
} from "@/pages/admin/adminUi";
import {
  checkHrUserMasterExists,
  createHrUserMaster,
  deleteHrUserMaster,
  getHrUserMasters,
  updateHrUserMaster,
} from "@/services/AdminService";

const blankHrForm = {
  employeeNo: "",
  name: "",
  email: "",
  phone: "",
  departmentCode: "",
  departmentName: "",
  position: "",
  employmentType: "EMPLOYEE",
  hrStatus: "ACTIVE",
  joinedAt: "",
  leftAt: "",
};

type HrForm = typeof blankHrForm;
type DuplicateState = Record<"employeeNo" | "email" | "phone", "idle" | "checking" | "available" | "duplicate" | "skipped">;

const isEmail = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
const formatPhone = (value: string) => {
  const digits = value.replace(/\D/g, "").slice(0, 11);
  if (digits.length <= 3) return digits;
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
};

export default function HrUserMasterPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = hasAdminAccess(user);
  const [items, setItems] = useState<HrUserMaster[]>([]);
  const [filters, setFilters] = useState({ keyword: "", accountStatus: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "employeeNo", direction: "ASC" });
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editing, setEditing] = useState<HrUserMaster | null>(null);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);
  const [form, setForm] = useState<HrForm>(blankHrForm);
  const [duplicates, setDuplicates] = useState<DuplicateState>({ employeeNo: "idle", email: "idle", phone: "idle" });
  const [formError, setFormError] = useState("");

  const load = async () => {
    setItems(await getHrUserMasters({ keyword: filters.keyword, accountStatus: filters.accountStatus }));
    setSelectedIds([]);
  };

  useEffect(() => {
    if (isAdmin) void load().catch(() => undefined);
  }, [isAdmin]);

  const filteredItems = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = items.filter((item) =>
      (!keyword || [item.employeeNo, item.name, item.email, item.phone, item.departmentCode, item.departmentName, item.position, item.accountUsername].some((value) => contains(value, keyword)))
        && (!filters.accountStatus || item.accountStatus === filters.accountStatus),
    );
    return [...filtered].sort((left, right) => {
      const direction = sortState.direction === "ASC" ? 1 : -1;
      const result = (() => {
        switch (sortState.sort) {
          case "name":
            return display(left.name).localeCompare(display(right.name));
          case "departmentName":
            return display(left.departmentName).localeCompare(display(right.departmentName));
          case "accountStatus":
            return display(left.accountStatus).localeCompare(display(right.accountStatus));
          case "employeeNo":
          default:
            return display(left.employeeNo).localeCompare(display(right.employeeNo));
        }
      })();
      return result * direction;
    });
  }, [items, filters, sortState]);

  const currentPageState = {
    ...pageState,
    totalElements: filteredItems.length,
    totalPages: Math.max(Math.ceil(filteredItems.length / pageState.size), 1),
  };
  const pagedItems = filteredItems.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size);
  const selectableItems = pagedItems.filter((item) => item.accountStatus === "NOT_CREATED");
  const selectedItems = items.filter((item) => selectedIds.includes(item.id));
  const allPageSelected = selectableItems.length > 0 && selectableItems.every((item) => selectedIds.includes(item.id));

  const toggleItem = (id: number, checked: boolean) => {
    setSelectedIds((current) => checked ? Array.from(new Set([...current, id])) : current.filter((item) => item !== id));
  };

  const togglePage = (checked: boolean) => {
    setSelectedIds(checked ? selectableItems.map((item) => item.id) : []);
  };

  const runBulkDelete = async () => {
    const targets = selectedItems.filter((item) => item.accountStatus === "NOT_CREATED");
    await Promise.all(targets.map((item) => deleteHrUserMaster(item.id)));
    setSelectedIds([]);
    setBulkDeleteOpen(false);
    await load();
  };

  const openCreate = () => {
    setEditing(null);
    setForm(blankHrForm);
    setDuplicates({ employeeNo: "idle", email: "idle", phone: "idle" });
    setFormError("");
    setModalMode("create");
  };

  const openEdit = (item: HrUserMaster) => {
    setEditing(item);
    setForm({
      employeeNo: item.employeeNo,
      name: item.name,
      email: item.email,
      phone: item.phone || "",
      departmentCode: item.departmentCode || "",
      departmentName: item.departmentName || "",
      position: item.position || "",
      employmentType: item.employmentType || "UNKNOWN",
      hrStatus: item.hrStatus || "ACTIVE",
      joinedAt: item.joinedAt || "",
      leftAt: item.leftAt || "",
    });
    setDuplicates({ employeeNo: "skipped", email: "skipped", phone: "skipped" });
    setFormError("");
    setModalMode("edit");
  };

  const handleSort = (column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
  };

  const updateForm = (key: keyof HrForm, value: string) => {
    const nextValue = key === "phone" ? formatPhone(value) : value;
    setForm((current) => ({ ...current, [key]: nextValue }));
    setFormError("");
    if (key === "employeeNo" || key === "email" || key === "phone") {
      setDuplicates((current) => ({ ...current, [key]: "idle" }));
    }
  };

  const checkDuplicate = async (field: keyof DuplicateState) => {
    const value = form[field].trim();
    if (!value) {
      setFormError(`${duplicateLabel(field)}을 입력한 뒤 중복확인하세요.`);
      return;
    }
    if (field === "email" && !isEmail(value)) {
      setFormError("이메일 형식이 올바르지 않습니다.");
      return;
    }
    if (modalMode === "edit" && editing && value === String(editing[field] ?? "")) {
      setDuplicates((current) => ({ ...current, [field]: "skipped" }));
      return;
    }

    setDuplicates((current) => ({ ...current, [field]: "checking" }));
    const result = await checkHrUserMasterExists(field, value);
    setDuplicates((current) => ({ ...current, [field]: result.exists ? "duplicate" : "available" }));
  };

  const validate = () => {
    if (!form.employeeNo.trim() || !form.name.trim() || !form.email.trim()) return "사번, 이름, 이메일은 필수입니다.";
    if (!isEmail(form.email.trim())) return "이메일 형식이 올바르지 않습니다.";
    if (form.phone && !/^010-\d{4}-\d{4}$/.test(form.phone)) return "전화번호는 010-0000-0000 형식으로 입력하세요.";
    if (modalMode === "create" && duplicates.employeeNo !== "available") return "사번 중복확인을 완료하세요.";
    if (duplicates.email === "duplicate" || duplicates.phone === "duplicate") return "중복된 이메일 또는 전화번호가 있습니다.";
    return "";
  };

  const save = async () => {
    const message = validate();
    if (message) {
      setFormError(message);
      return;
    }
    const payload: HrUserMasterRequest = { ...form };
    if (modalMode === "edit" && editing) {
      await updateHrUserMaster(editing.id, payload);
    } else {
      await createHrUserMaster(payload);
    }
    setModalMode(null);
    await load();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="HR 기준정보 관리"
      description="계정 생성 기준이 되는 직원 원장을 등록하고 계정 생성 상태를 확인합니다."
      actions={<Button type="button" onClick={openCreate}><Plus className="h-4 w-4" />직원 등록</Button>}
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "직원 검색", placeholder: "사번, 이름, 이메일, 부서" },
          {
            name: "accountStatus",
            label: "계정 상태",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "미생성", value: "NOT_CREATED" },
              { label: "생성됨", value: "CREATED" },
              { label: "비활성", value: "DISABLED" },
            ],
          },
        ]}
        values={filters}
        onChange={(name, value) => {
          setFilters((current) => ({ ...current, [name]: value }));
          setPageState((current) => ({ ...current, page: 0 }));
        }}
        onSubmit={() => void load().catch(() => undefined)}
        onReset={() => {
          setFilters({ keyword: "", accountStatus: "" });
          setPageState((current) => ({ ...current, page: 0 }));
        }}
      />

      <AdminBulkActionBar selectedLabel={`선택 ${selectedIds.length}건`}>
        <Button type="button" variant="destructive" disabled={selectedIds.length === 0} onClick={() => setBulkDeleteOpen(true)}>선택 삭제</Button>
      </AdminBulkActionBar>

      <AdminTableCard>
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <input type="checkbox" aria-label="현재 페이지 HR 기준정보 전체 선택" checked={allPageSelected} onChange={(event) => togglePage(event.target.checked)} />
                </th>
                <th className={adminCellClassName}><AdminSortableHeader label="사번" column="employeeNo" sortState={sortState} onSort={handleSort} /></th>
                <th className={adminCellClassName}><AdminSortableHeader label="이름" column="name" sortState={sortState} onSort={handleSort} /></th>
                <th className={adminCellClassName}>이메일</th>
                <th className={adminCellClassName}>전화번호</th>
                <th className={adminCellClassName}><AdminSortableHeader label="부서" column="departmentName" sortState={sortState} onSort={handleSort} /></th>
                <th className={adminCellClassName}>고용형태</th>
                <th className={adminCellClassName}>HR 상태</th>
                <th className={adminCellClassName}><AdminSortableHeader label="계정 상태" column="accountStatus" sortState={sortState} onSort={handleSort} /></th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {pagedItems.length === 0 ? <AdminEmptyRow colSpan={10} /> : null}
              {pagedItems.map((item) => (
                <tr key={item.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <input
                      type="checkbox"
                      aria-label={`${item.employeeNo} 선택`}
                      disabled={item.accountStatus !== "NOT_CREATED"}
                      checked={selectedIds.includes(item.id)}
                      onChange={(event) => toggleItem(item.id, event.target.checked)}
                    />
                  </td>
                  <td className={adminCellClassName}>{item.employeeNo}</td>
                  <td className={adminCellClassName}>{item.name}</td>
                  <td className={adminCellClassName}>{item.email}</td>
                  <td className={adminCellClassName}>{display(item.phone)}</td>
                  <td className={adminCellClassName}>
                    {display(item.departmentName)}
                    <div className="text-xs text-muted-foreground">{display(item.position)}</div>
                  </td>
                  <td className={adminCellClassName}>{display(item.employmentType)}</td>
                  <td className={adminCellClassName}><AdminBadge tone={statusTone(item.hrStatus)}>{item.hrStatus}</AdminBadge></td>
                  <td className={adminCellClassName}><AdminBadge tone={statusTone(item.accountStatus)}>{item.accountStatus}</AdminBadge></td>
                  <td className={adminCellClassName}>
                    <div className="flex flex-wrap justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openEdit(item)}>수정</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={currentPageState} onPageChange={(page) => setPageState((current) => ({ ...current, page }))} />
      </AdminTableCard>

      <AdminCrudModal
        open={modalMode !== null}
        title={modalMode === "create" ? "직원 등록" : "직원 기준정보 수정"}
        description="계정 생성에 사용할 최소 HR 기준정보를 입력합니다."
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
        <CheckableField label="사번" value={form.employeeNo} disabled={modalMode === "edit"} state={duplicates.employeeNo} onCheck={() => void checkDuplicate("employeeNo")} onChange={(value) => updateForm("employeeNo", value)} />
        <AdminFormField label="이름" value={form.name} onChange={(value) => updateForm("name", value)} />
        <CheckableField label="이메일" type="email" value={form.email} state={duplicates.email} onCheck={() => void checkDuplicate("email")} onChange={(value) => updateForm("email", value)} />
        <CheckableField label="전화번호" value={form.phone} state={duplicates.phone} placeholder="010-0000-0000" onCheck={() => void checkDuplicate("phone")} onChange={(value) => updateForm("phone", value)} />
        <AdminFormField label="부서 코드" value={form.departmentCode} onChange={(value) => updateForm("departmentCode", value)} />
        <AdminFormField label="부서명" value={form.departmentName} onChange={(value) => updateForm("departmentName", value)} />
        <AdminFormField label="직책" value={form.position} onChange={(value) => updateForm("position", value)} />
        <AdminSelectField
          label="고용형태"
          value={form.employmentType}
          options={[
            { label: "정규직", value: "EMPLOYEE" },
            { label: "계약직", value: "CONTRACTOR" },
            { label: "외부", value: "EXTERNAL" },
            { label: "미지정", value: "UNKNOWN" },
          ]}
          onChange={(value) => updateForm("employmentType", value)}
        />
        <AdminSelectField
          label="HR 상태"
          value={form.hrStatus}
          options={[
            { label: "재직", value: "ACTIVE" },
            { label: "휴직", value: "LEAVE" },
            { label: "퇴직", value: "RETIRED" },
            { label: "정지", value: "SUSPENDED" },
          ]}
          onChange={(value) => updateForm("hrStatus", value)}
        />
        <AdminFormField label="입사일" type="date" value={form.joinedAt} onChange={(value) => updateForm("joinedAt", value)} />
        <AdminFormField label="퇴사일" type="date" value={form.leftAt} onChange={(value) => updateForm("leftAt", value)} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={bulkDeleteOpen}
        title="HR 기준정보 삭제"
        description={`${selectedIds.length}건의 HR 기준정보를 삭제합니다. 계정이 생성된 직원은 선택할 수 없습니다.`}
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

function duplicateLabel(field: keyof DuplicateState) {
  return field === "employeeNo" ? "사번" : field === "email" ? "이메일" : "전화번호";
}

function duplicateMessage(state: DuplicateState[keyof DuplicateState]) {
  if (state === "available") return "사용 가능";
  if (state === "duplicate") return "이미 사용 중";
  if (state === "checking") return "확인 중";
  if (state === "skipped") return "기존 값";
  return "중복확인 필요";
}

function CheckableField({
  label,
  value,
  state,
  onCheck,
  onChange,
  type = "text",
  disabled = false,
  placeholder,
}: {
  label: string;
  value: string;
  state: DuplicateState[keyof DuplicateState];
  onCheck: () => void;
  onChange: (value: string) => void;
  type?: string;
  disabled?: boolean;
  placeholder?: string;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <div className="flex gap-2">
        <Input type={type} value={value} disabled={disabled} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} />
        <Button type="button" variant="outline" disabled={disabled || state === "checking"} onClick={onCheck}>
          <SearchCheck className="h-4 w-4" />확인
        </Button>
      </div>
      <p className={state === "duplicate" ? "text-xs text-destructive" : "text-xs text-muted-foreground"}>{duplicateMessage(state)}</p>
    </div>
  );
}
