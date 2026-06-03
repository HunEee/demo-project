import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatSecurityDateTime } from "@/lib/dateTime";
import type { AdminUser, PageResponse } from "@/models/AdminModels";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { AdminBadge, AdminConfirmDialog, AdminCrudModal, AdminEmptyRow, AdminPagination, type PageState, adminCellClassName, adminRowClassName, adminTableClassName, adminTheadClassName, statusTone } from "@/pages/admin/adminUi";
import { createAdminUser, deleteAdminUser, getAdminUsers, updateAdminUser } from "@/services/AdminService";

const blankExternalForm = {
  username: "",
  password: "",
  email: "",
  name: "",
  employeeNo: "",
  departmentId: "",
  position: "",
  expiresAt: "",
  roleName: "ROLE_USER",
  reason: "",
};

export default function ExternalUsersPage() {
  const user = useAuth((state) => state.user);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const [filters, setFilters] = useState({ keyword: "", expiresBefore: "" });
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [drawerMode, setDrawerMode] = useState<"create" | "edit" | null>(null);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [form, setForm] = useState(blankExternalForm);
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null);

  const applyPage = (page: PageResponse<AdminUser>) => {
    setUsers(page.content);
    setPageState({ page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements });
  };

  const load = async (nextPage = pageState.page) => {
    applyPage(
      await getAdminUsers({
        keyword: filters.keyword,
        employmentType: "EXTERNAL",
        expiresBefore: filters.expiresBefore,
        page: nextPage,
        size: pageState.size,
        sort: "createdAt",
        direction: "DESC",
      }),
    );
  };

  useEffect(() => {
    if (isAdmin) void load(0).catch(() => undefined);
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell
      title="외부 사용자 관리"
      description="외부 사용자와 계약 만료 예정 계정을 조회합니다."
      actions={
        <Button
          type="button"
          onClick={() => {
            setSelectedUser(null);
            setForm(blankExternalForm);
            setDrawerMode("create");
          }}
        >
          외부 사용자 추가
        </Button>
      }
    >
      <AdminFilters
        fields={[
          { name: "keyword", label: "외부 사용자 검색", placeholder: "아이디, 이메일, 사번, 부서" },
          { name: "expiresBefore", label: "만료일 이전", type: "date" },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0).catch(() => undefined)}
        onReset={() => {
          setFilters({ keyword: "", expiresBefore: "" });
          void load(0).catch(() => undefined);
        }}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>사용자 ID</th>
                <th className={adminCellClassName}>이름</th>
                <th className={adminCellClassName}>이메일</th>
                <th className={adminCellClassName}>사번</th>
                <th className={adminCellClassName}>부서</th>
                <th className={adminCellClassName}>상태</th>
                <th className={adminCellClassName}>만료일</th>
                <th className={adminCellClassName}>마지막 로그인</th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? <AdminEmptyRow colSpan={9} /> : null}
              {users.map((item) => (
                <tr key={item.username} className={adminRowClassName}>
                  <td className={adminCellClassName}>
                    <Link className="font-medium text-primary hover:underline" to={`/admin/account/users/${item.username}`}>
                      {item.username}
                    </Link>
                  </td>
                  <td className={adminCellClassName}>{item.name || item.nickname || "-"}</td>
                  <td className={adminCellClassName}>{item.email || "-"}</td>
                  <td className={adminCellClassName}>{item.employeeNo || "-"}</td>
                  <td className={adminCellClassName}>{item.department || "-"}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(item.status)}>{item.status || "ACTIVE"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{item.expiresAt ? formatSecurityDateTime(item.expiresAt) : "-"}</td>
                  <td className={adminCellClassName}>{item.lastLoginAt ? formatSecurityDateTime(item.lastLoginAt) : "-"}</td>
                  <td className={adminCellClassName}>
                    <div className="flex flex-wrap justify-center gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setSelectedUser(item);
                          setForm({
                            username: item.username,
                            password: "",
                            email: item.email || "",
                            name: item.name || item.nickname || "",
                            employeeNo: item.employeeNo || "",
                            departmentId: item.departmentId ? String(item.departmentId) : "",
                            position: item.position || "",
                            expiresAt: item.expiresAt ? item.expiresAt.slice(0, 10) : "",
                            roleName: item.roles?.[0] || "ROLE_USER",
                            reason: "",
                          });
                          setDrawerMode("edit");
                        }}
                      >
                        수정
                      </Button>
                      <Button size="sm" variant="destructive" onClick={() => setDeleteTarget(item)}>
                        삭제
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={pageState} onPageChange={(page) => void load(page).catch(() => undefined)} />
        </CardContent>
      </Card>

      <AdminCrudModal
        open={drawerMode !== null}
        title={drawerMode === "create" ? "외부 사용자 추가" : "외부 사용자 수정"}
        description="외부 사용자 기본 정보와 만료일을 입력합니다."
        onOpenChange={(open) => {
          if (!open) setDrawerMode(null);
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={() => setDrawerMode(null)}>
              취소
            </Button>
            <Button
              type="button"
              onClick={async () => {
                const payload = {
                  email: form.email,
                  name: form.name,
                  employeeNo: form.employeeNo,
                  departmentId: form.departmentId ? Number(form.departmentId) : null,
                  position: form.position,
                  employmentType: "EXTERNAL",
                  status: "ACTIVE",
                  expiresAt: form.expiresAt,
                  reason: form.reason,
                };
                if (drawerMode === "create") {
                  await createAdminUser({ ...payload, username: form.username, password: form.password, roleName: form.roleName });
                } else if (selectedUser) {
                  await updateAdminUser(selectedUser.username, payload);
                }
                setDrawerMode(null);
                await load(0);
              }}
            >
              저장
            </Button>
          </>
        }
      >
        {drawerMode === "create" ? <Field label="사용자 ID" value={form.username} onChange={(value) => setForm((prev) => ({ ...prev, username: value }))} /> : null}
        {drawerMode === "create" ? <Field label="초기 비밀번호" type="password" value={form.password} onChange={(value) => setForm((prev) => ({ ...prev, password: value }))} /> : null}
        <Field label="이름" value={form.name} onChange={(value) => setForm((prev) => ({ ...prev, name: value }))} />
        <Field label="이메일" type="email" value={form.email} onChange={(value) => setForm((prev) => ({ ...prev, email: value }))} />
        <Field label="사번" value={form.employeeNo} onChange={(value) => setForm((prev) => ({ ...prev, employeeNo: value }))} />
        <Field label="부서 ID" type="number" value={form.departmentId} onChange={(value) => setForm((prev) => ({ ...prev, departmentId: value }))} />
        <Field label="직급" value={form.position} onChange={(value) => setForm((prev) => ({ ...prev, position: value }))} />
        <Field label="만료일" type="date" value={form.expiresAt} onChange={(value) => setForm((prev) => ({ ...prev, expiresAt: value }))} />
        {drawerMode === "create" ? <Field label="초기 역할" value={form.roleName} onChange={(value) => setForm((prev) => ({ ...prev, roleName: value }))} /> : null}
        <Field label="사유" value={form.reason} onChange={(value) => setForm((prev) => ({ ...prev, reason: value }))} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={deleteTarget !== null}
        title="외부 사용자 삭제"
        description={`${deleteTarget?.username ?? ""} 외부 사용자 계정을 삭제 처리하고 접근을 차단합니다.`}
        confirmLabel="삭제"
        destructive
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        onConfirm={() => {
          if (!deleteTarget) return;
          void deleteAdminUser(deleteTarget.username, "외부 사용자 삭제").then(() => load());
        }}
      />
    </AdminPageShell>
  );
}

function Field({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-muted-foreground">{label}</Label>
      <Input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}
