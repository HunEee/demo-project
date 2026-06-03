import { useEffect, useMemo, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AdminDepartment, AdminDepartmentRequest, AdminDepartmentUser, AdminDepartmentUserRequest } from "@/models/AdminModels";
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
  addAdminDepartmentUser,
  createAdminDepartment,
  disableAdminDepartment,
  getAdminDepartmentUsers,
  getAdminDepartments,
  getAdminUsers,
  removeAdminDepartmentUser,
  updateAdminDepartment,
  updateAdminDepartmentUser,
} from "@/services/AdminService";

const blankDepartment = { name: "", code: "", managerUsername: "", displayOrder: "0", reason: "" };
const blankDepartmentUser = {
  username: "",
  employeeNo: "",
  position: "",
  employmentType: "EMPLOYEE",
  status: "ACTIVE",
  expiresAt: "",
  reason: "",
};

const contains = (value: string | number | null | undefined, keyword: string) =>
  String(value ?? "").toLowerCase().includes(keyword.trim().toLowerCase());

const compareText = (left?: string | null, right?: string | null) => String(left ?? "").localeCompare(String(right ?? ""));
const display = (value?: string | number | null) => (value === null || value === undefined || value === "" ? "-" : String(value));
const toDateInput = (value?: string | null) => (value ? value.slice(0, 10) : "");

export default function OrganizationPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [departments, setDepartments] = useState<AdminDepartment[]>([]);
  const [filters, setFilters] = useState({ keyword: "", status: "", manager: "" });
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "displayOrder", direction: "ASC" });
  const [departmentForm, setDepartmentForm] = useState(blankDepartment);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editDepartment, setEditDepartment] = useState<AdminDepartment | null>(null);
  const [disableTarget, setDisableTarget] = useState<AdminDepartment | null>(null);
  const [detailDepartment, setDetailDepartment] = useState<AdminDepartment | null>(null);
  const [departmentUsers, setDepartmentUsers] = useState<AdminDepartmentUser[]>([]);
  const [departmentUserFilters, setDepartmentUserFilters] = useState({ keyword: "", status: "", employmentType: "" });
  const [departmentUserForm, setDepartmentUserForm] = useState(blankDepartmentUser);
  const [departmentUserMode, setDepartmentUserMode] = useState<"add" | "edit">("add");
  const [departmentUserMessage, setDepartmentUserMessage] = useState("");
  const [removeUserTarget, setRemoveUserTarget] = useState<AdminDepartmentUser | null>(null);

  const loadDepartments = async () => {
    const next = await getAdminDepartments();
    setDepartments(next);
    setDetailDepartment((current) => (current ? next.find((item) => item.id === current.id) ?? current : current));
    setEditDepartment((current) => (current ? next.find((item) => item.id === current.id) ?? current : current));
  };

  const loadDepartmentUsers = async (departmentId: number) => {
    setDepartmentUsers(await getAdminDepartmentUsers(departmentId));
  };

  useEffect(() => {
    if (isAdmin) void loadDepartments().catch(() => undefined);
  }, [isAdmin]);

  useEffect(() => {
    if (isAdmin && detailDepartment) void loadDepartmentUsers(detailDepartment.id).catch(() => undefined);
  }, [isAdmin, detailDepartment?.id]);

  const filteredDepartments = useMemo(() => {
    const keyword = filters.keyword.trim();
    const filtered = departments.filter((department) => {
      const status = department.enabled ? "ACTIVE" : "DISABLED";
      return (!keyword || [department.name, department.code, department.parentName, department.managerUsername].some((value) => contains(value, keyword)))
        && (!filters.status || status === filters.status)
        && (!filters.manager || contains(department.managerUsername, filters.manager));
    });

    return [...filtered].sort((left, right) => {
      const direction = sortState.direction === "ASC" ? 1 : -1;
      const result = (() => {
        switch (sortState.sort) {
          case "name":
            return compareText(left.name, right.name);
          case "code":
            return compareText(left.code, right.code);
          case "managerUsername":
            return compareText(left.managerUsername, right.managerUsername);
          case "userCount":
            return left.userCount - right.userCount;
          case "enabled":
            return Number(left.enabled) - Number(right.enabled);
          case "displayOrder":
          default:
            return left.displayOrder - right.displayOrder || compareText(left.name, right.name);
        }
      })();
      return result * direction;
    });
  }, [departments, filters, sortState]);

  const departmentListPageState = {
    ...pageState,
    totalElements: filteredDepartments.length,
    totalPages: Math.max(Math.ceil(filteredDepartments.length / pageState.size), 1),
  };
  const pagedDepartments = filteredDepartments.slice(pageState.page * pageState.size, pageState.page * pageState.size + pageState.size);

  const filteredDepartmentUsers = useMemo(() => {
    const keyword = departmentUserFilters.keyword.trim();
    return departmentUsers.filter((item) => {
      return (!keyword || [item.username, item.name, item.email, item.employeeNo, item.position].some((value) => contains(value, keyword)))
        && (!departmentUserFilters.status || item.status === departmentUserFilters.status)
        && (!departmentUserFilters.employmentType || item.employmentType === departmentUserFilters.employmentType);
    });
  }, [departmentUsers, departmentUserFilters]);

  const handleSort = (column: string) => {
    setSortState((current) => ({
      sort: column,
      direction: current.sort === column && current.direction === "ASC" ? "DESC" : "ASC",
    }));
    setPageState((current) => ({ ...current, page: 0 }));
  };

  const openDepartmentModal = (mode: "create" | "edit", department?: AdminDepartment) => {
    setModalMode(mode);
    setEditDepartment(department ?? null);
    setDepartmentForm(
      department
        ? {
            name: department.name,
            code: department.code,
            managerUsername: department.managerUsername || "",
            displayOrder: String(department.displayOrder),
            reason: "",
          }
        : blankDepartment,
    );
  };

  const openDepartmentDetail = async (department: AdminDepartment) => {
    setDetailDepartment(department);
    setDepartmentUserFilters({ keyword: "", status: "", employmentType: "" });
    setDepartmentUserForm(blankDepartmentUser);
    setDepartmentUserMode("add");
    setDepartmentUserMessage("");
    await loadDepartmentUsers(department.id);
  };

  const resetDepartmentUserForm = () => {
    setDepartmentUserForm(blankDepartmentUser);
    setDepartmentUserMode("add");
    setDepartmentUserMessage("");
  };

  const validateExistingUser = async (username: string) => {
    const trimmed = username.trim();
    if (!trimmed) {
      setDepartmentUserMessage("추가할 사용자 ID를 입력하세요.");
      return false;
    }
    const page = await getAdminUsers({ keyword: trimmed, page: 0, size: 10, sort: "username", direction: "ASC" });
    const exactUser = page.content.find((item) => item.username === trimmed && !item.deleted);
    if (!exactUser) {
      setDepartmentUserMessage("기존에 등록된 사용자 ID만 부서에 추가할 수 있습니다.");
      return false;
    }
    return true;
  };

  const saveDepartmentUser = async () => {
    if (!detailDepartment) return;
    const username = departmentUserForm.username.trim();
    const payload: AdminDepartmentUserRequest = {
      username,
      employeeNo: departmentUserForm.employeeNo,
      position: departmentUserForm.position,
      employmentType: departmentUserForm.employmentType,
      status: departmentUserForm.status,
      expiresAt: departmentUserForm.expiresAt,
      reason: departmentUserForm.reason || (departmentUserMode === "add" ? "부서 사용자 추가" : "부서 사용자 수정"),
    };

    if (departmentUserMode === "add") {
      if (!(await validateExistingUser(username))) return;
      await addAdminDepartmentUser(detailDepartment.id, payload);
    } else {
      await updateAdminDepartmentUser(detailDepartment.id, username, payload);
    }
    resetDepartmentUserForm();
    await loadDepartmentUsers(detailDepartment.id);
    await loadDepartments();
  };

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="조직 / 부서 관리"
      description="부서를 목록으로 조회하고, 부서 상세 모달에서 소속 사용자를 관리합니다."
      actions={
        <Button type="button" onClick={() => openDepartmentModal("create")}>
          부서 추가
        </Button>
      }
    >
      <AdminFilters
        hint="부서 목록 필터 적용"
        fields={[
          { name: "keyword", label: "부서 검색", placeholder: "부서명, 코드, 상위 부서" },
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
          { name: "manager", label: "부서장", placeholder: "부서장 사용자 ID" },
        ]}
        values={filters}
        onChange={(name, value) => {
          setFilters((current) => ({ ...current, [name]: value }));
          setPageState((current) => ({ ...current, page: 0 }));
        }}
        onSubmit={() => setPageState((current) => ({ ...current, page: 0 }))}
        onReset={() => {
          setFilters({ keyword: "", status: "", manager: "" });
          setPageState((current) => ({ ...current, page: 0 }));
        }}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="부서명" column="name" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="코드" column="code" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>상위 부서</th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="부서장" column="managerUsername" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자 수" column="userCount" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="enabled" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {pagedDepartments.length === 0 ? <AdminEmptyRow colSpan={7} /> : null}
              {pagedDepartments.map((department) => (
                <tr key={department.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <button type="button" className="font-medium text-primary hover:underline" onClick={() => void openDepartmentDetail(department)}>
                      {department.name}
                    </button>
                  </td>
                  <td className={adminCellClassName}>{department.code}</td>
                  <td className={adminCellClassName}>{department.parentName || "-"}</td>
                  <td className={adminCellClassName}>{department.managerUsername || "-"}</td>
                  <td className={adminCellClassName}>{department.userCount}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(department.enabled)}>{department.enabled ? "ACTIVE" : "DISABLED"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <div className="flex justify-center gap-2">
                      <Button size="sm" variant="outline" onClick={() => openDepartmentModal("edit", department)}>
                        수정
                      </Button>
                      <Button size="sm" variant="destructive" onClick={() => setDisableTarget(department)}>
                        삭제
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={departmentListPageState} onPageChange={(page) => setPageState((current) => ({ ...current, page }))} />
        </CardContent>
      </Card>

      <AdminCrudModal
        open={modalMode !== null}
        title={modalMode === "create" ? "부서 추가" : "부서 수정"}
        description="부서 기본 정보와 부서장을 입력합니다."
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
                const payload: AdminDepartmentRequest = {
                  name: departmentForm.name,
                  code: departmentForm.code,
                  managerUsername: departmentForm.managerUsername || null,
                  enabled: true,
                  displayOrder: Number(departmentForm.displayOrder || 0),
                  reason: departmentForm.reason || (modalMode === "create" ? "부서 생성" : "부서 수정"),
                };
                if (modalMode === "edit" && editDepartment) {
                  await updateAdminDepartment(editDepartment.id, payload);
                } else {
                  await createAdminDepartment(payload);
                }
                setModalMode(null);
                setDepartmentForm(blankDepartment);
                await loadDepartments();
              }}
            >
              저장
            </Button>
          </>
        }
      >
        <Field label="부서명" value={departmentForm.name} onChange={(value) => setDepartmentForm((current) => ({ ...current, name: value }))} />
        <Field label="부서 코드" value={departmentForm.code} onChange={(value) => setDepartmentForm((current) => ({ ...current, code: value }))} />
        <Field label="부서장 사용자 ID" value={departmentForm.managerUsername} onChange={(value) => setDepartmentForm((current) => ({ ...current, managerUsername: value }))} />
        <Field label="정렬 순서" type="number" value={departmentForm.displayOrder} onChange={(value) => setDepartmentForm((current) => ({ ...current, displayOrder: value }))} />
        <Field label="사유" value={departmentForm.reason} onChange={(value) => setDepartmentForm((current) => ({ ...current, reason: value }))} />
      </AdminCrudModal>

      <AdminCrudModal
        open={detailDepartment !== null}
        title={detailDepartment ? `${detailDepartment.name} 상세` : "부서 상세"}
        description="부서 정보와 소속 사용자를 조회하고 관리합니다."
        contentClassName="sm:max-w-[960px]"
        onOpenChange={(open) => {
          if (!open) {
            setDetailDepartment(null);
            resetDepartmentUserForm();
          }
        }}
      >
        {detailDepartment ? (
          <div className="space-y-5">
            <div className="grid gap-3 rounded-lg border p-3 text-sm sm:grid-cols-2 lg:grid-cols-5">
              <Info label="부서명" value={detailDepartment.name} />
              <Info label="코드" value={detailDepartment.code} />
              <Info label="상위 부서" value={detailDepartment.parentName || "-"} />
              <Info label="부서장" value={detailDepartment.managerUsername || "-"} />
              <Info label="소속 사용자" value={`${detailDepartment.userCount}명`} />
            </div>

            <div className="rounded-lg border p-3">
              <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h3 className="font-medium">부서 사용자 {departmentUserMode === "add" ? "추가" : "수정"}</h3>
                  <p className="text-xs text-muted-foreground">사용자 추가는 기존에 등록된 사용자 ID만 가능합니다.</p>
                </div>
                {departmentUserMode === "edit" ? (
                  <Button type="button" variant="outline" size="sm" onClick={resetDepartmentUserForm}>
                    추가 모드로 전환
                  </Button>
                ) : null}
              </div>
              <div className="grid gap-3 md:grid-cols-3">
                <Field
                  label="사용자 ID"
                  value={departmentUserForm.username}
                  disabled={departmentUserMode === "edit"}
                  onChange={(value) => setDepartmentUserForm((current) => ({ ...current, username: value }))}
                />
                <Field label="사번" value={departmentUserForm.employeeNo} onChange={(value) => setDepartmentUserForm((current) => ({ ...current, employeeNo: value }))} />
                <Field label="직급" value={departmentUserForm.position} onChange={(value) => setDepartmentUserForm((current) => ({ ...current, position: value }))} />
                <SelectField
                  label="고용 형태"
                  value={departmentUserForm.employmentType}
                  options={[
                    { label: "정규직", value: "EMPLOYEE" },
                    { label: "계약직", value: "CONTRACTOR" },
                    { label: "외부", value: "EXTERNAL" },
                    { label: "미지정", value: "UNKNOWN" },
                  ]}
                  onChange={(value) => setDepartmentUserForm((current) => ({ ...current, employmentType: value }))}
                />
                <SelectField
                  label="상태"
                  value={departmentUserForm.status}
                  options={[
                    { label: "활성", value: "ACTIVE" },
                    { label: "잠금", value: "LOCKED" },
                    { label: "비활성", value: "DISABLED" },
                    { label: "만료", value: "EXPIRED" },
                    { label: "휴직", value: "LEAVE" },
                  ]}
                  onChange={(value) => setDepartmentUserForm((current) => ({ ...current, status: value }))}
                />
                <Field label="만료일" type="date" value={departmentUserForm.expiresAt} onChange={(value) => setDepartmentUserForm((current) => ({ ...current, expiresAt: value }))} />
                <Field label="사유" value={departmentUserForm.reason} onChange={(value) => setDepartmentUserForm((current) => ({ ...current, reason: value }))} />
              </div>
              {departmentUserMessage ? <p className="mt-2 text-sm text-red-600">{departmentUserMessage}</p> : null}
              <div className="mt-3 flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={resetDepartmentUserForm}>
                  초기화
                </Button>
                <Button type="button" onClick={() => void saveDepartmentUser()}>
                  {departmentUserMode === "add" ? "기존 사용자 추가" : "수정 저장"}
                </Button>
              </div>
            </div>

            <AdminFilters
              hint="부서 사용자 필터 적용"
              fields={[
                { name: "keyword", label: "사용자 검색", placeholder: "ID, 이름, 이메일, 사번" },
                {
                  name: "status",
                  label: "상태",
                  type: "select",
                  options: [
                    { label: "전체", value: "" },
                    { label: "활성", value: "ACTIVE" },
                    { label: "잠금", value: "LOCKED" },
                    { label: "비활성", value: "DISABLED" },
                    { label: "만료", value: "EXPIRED" },
                    { label: "휴직", value: "LEAVE" },
                  ],
                },
                {
                  name: "employmentType",
                  label: "고용 형태",
                  type: "select",
                  options: [
                    { label: "전체", value: "" },
                    { label: "정규직", value: "EMPLOYEE" },
                    { label: "계약직", value: "CONTRACTOR" },
                    { label: "외부", value: "EXTERNAL" },
                    { label: "미지정", value: "UNKNOWN" },
                  ],
                },
              ]}
              values={departmentUserFilters}
              onChange={(name, value) => setDepartmentUserFilters((current) => ({ ...current, [name]: value }))}
              onSubmit={() => undefined}
              onReset={() => setDepartmentUserFilters({ keyword: "", status: "", employmentType: "" })}
            />

            <div className="overflow-x-auto rounded-lg border">
              <table className={adminTableClassName}>
                <thead className={adminTheadClassName}>
                  <tr>
                    <th className={adminCellClassName}>사용자 ID</th>
                    <th className={adminCellClassName}>이름</th>
                    <th className={adminCellClassName}>이메일</th>
                    <th className={adminCellClassName}>사번</th>
                    <th className={adminCellClassName}>직급</th>
                    <th className={adminCellClassName}>상태</th>
                    <th className={adminCellClassName}>고용 형태</th>
                    <th className={adminCellClassName}>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDepartmentUsers.length === 0 ? <AdminEmptyRow colSpan={8} /> : null}
                  {filteredDepartmentUsers.map((item) => (
                    <tr key={item.username} className={adminRowClassName}>
                      <td className={adminCellClassName}>{item.username}</td>
                      <td className={adminCellClassName}>{display(item.name)}</td>
                      <td className={adminCellClassName}>{display(item.email)}</td>
                      <td className={adminCellClassName}>{display(item.employeeNo)}</td>
                      <td className={adminCellClassName}>{display(item.position)}</td>
                      <td className={adminCellClassName}>
                        <AdminBadge tone={statusTone(item.status)}>{display(item.status)}</AdminBadge>
                      </td>
                      <td className={adminCellClassName}>{display(item.employmentType)}</td>
                      <td className={adminCellClassName}>
                        <div className="flex justify-center gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                              setDepartmentUserMode("edit");
                              setDepartmentUserMessage("");
                              setDepartmentUserForm({
                                username: item.username,
                                employeeNo: item.employeeNo || "",
                                position: item.position || "",
                                employmentType: item.employmentType || "UNKNOWN",
                                status: item.status || "ACTIVE",
                                expiresAt: toDateInput(item.expiresAt),
                                reason: "",
                              });
                            }}
                          >
                            수정
                          </Button>
                          <Button size="sm" variant="destructive" onClick={() => setRemoveUserTarget(item)}>
                            삭제
                          </Button>
                        </div>
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
        open={disableTarget !== null}
        title="부서 삭제"
        description={`${disableTarget?.name ?? ""} 부서를 비활성화합니다. 소속 사용자 접근 정책을 먼저 확인하세요.`}
        confirmLabel="비활성화"
        destructive
        onOpenChange={(open) => {
          if (!open) setDisableTarget(null);
        }}
        onConfirm={() => {
          if (!disableTarget) return;
          void disableAdminDepartment(disableTarget.id, "부서 비활성화").then(loadDepartments).catch(() => undefined);
        }}
      />

      <AdminConfirmDialog
        open={removeUserTarget !== null}
        title="부서 사용자 삭제"
        description={`${removeUserTarget?.username ?? ""} 사용자를 현재 부서에서 제거합니다. 계정 자체는 삭제되지 않습니다.`}
        confirmLabel="부서에서 제거"
        destructive
        onOpenChange={(open) => {
          if (!open) setRemoveUserTarget(null);
        }}
        onConfirm={() => {
          if (!detailDepartment || !removeUserTarget) return;
          void removeAdminDepartmentUser(detailDepartment.id, removeUserTarget.username, "부서 사용자 제거").then(async () => {
            await loadDepartmentUsers(detailDepartment.id);
            await loadDepartments();
            resetDepartmentUserForm();
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
  type = "text",
  disabled = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  disabled?: boolean;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <Input type={type} value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)} />
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
