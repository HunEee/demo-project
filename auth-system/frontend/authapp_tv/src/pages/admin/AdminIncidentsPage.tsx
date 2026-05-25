import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import { CheckCircle2 } from "lucide-react";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { formatSecurityDateTime } from "@/lib/dateTime";
import AdminFilters from "@/pages/admin/AdminFilters";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import {
  AdminBadge,
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
import type { AdminFilterOptions, AdminIncident } from "@/models/AdminModels";
import { getAdminFilterOptions, getAdminIncidents, resolveAdminIncident } from "@/services/AdminService";

export default function AdminIncidentsPage() {
  const user = useAuth((state) => state.user);
  const [items, setItems] = useState<AdminIncident[]>([]);
  const [filters, setFilters] = useState({ username: "", type: "", severity: "", resolved: "", from: "", to: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "createdAt", direction: "DESC" });
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");
  const load = async (nextPage = pageState.page, nextSort = sortState) => {
    const page = await getAdminIncidents({
          username: filters.username,
          type: filters.type,
          severity: filters.severity,
          resolved: filters.resolved,
          from: filters.from,
          to: filters.to,
          page: nextPage,
          size: pageState.size,
          sort: nextSort.sort,
          direction: nextSort.direction,
        });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  const handleSort = (column: string) => {
    const nextSort: SortState = {
      sort: column,
      direction: sortState.sort === column && sortState.direction === "DESC" ? "ASC" : "DESC",
    };
    setSortState(nextSort);
    void load(0, nextSort);
  };

  const resetFilters = async () => {
    const nextFilters = { username: "", type: "", severity: "", resolved: "", from: "", to: "" };
    setFilters(nextFilters);
    const page = await getAdminIncidents({ username: nextFilters.username, type: nextFilters.type, severity: nextFilters.severity, resolved: nextFilters.resolved, from: nextFilters.from, to: nextFilters.to, page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  useEffect(() => {
    if (isAdmin) {
      void load();
      void getAdminFilterOptions().then(setFilterOptions);
    }
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell title="보안 사고 관리" description="미해결 사고를 확인하고 해결 상태로 전환합니다.">
      <AdminFilters
        fields={[
          { name: "username", label: "사용자 검색", placeholder: "아이디" },
          { name: "type", label: "사고 유형", type: "select", options: [{ label: "전체", value: "" }, ...(filterOptions?.incidentTypes ?? [])] },
          {
            name: "severity",
            label: "심각도",
            type: "select",
            options: [{ label: "전체", value: "" }, ...(filterOptions?.incidentSeverities ?? [])],
          },
          {
            name: "resolved",
            label: "해결 상태",
            type: "select",
            options: [
              { label: "전체", value: "" },
              { label: "미해결", value: "false" },
              { label: "해결됨", value: "true" },
            ],
          },
          { name: "from", label: "시작일", type: "date" },
          { name: "to", label: "종료일", type: "date" },
        ]}
        values={filters}
        onChange={(name, value) => setFilters((prev) => ({ ...prev, [name]: value }))}
        onSubmit={() => void load(0)}
        onReset={() => void resetFilters()}
      />

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="시간" column="createdAt" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="유형" column="type" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="심각도" column="severity" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="상태" column="resolved" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>작업</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? <AdminEmptyRow colSpan={6} /> : null}
              {items.map((item) => (
                <tr key={item.id} className={adminRowClassName}>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(item.createdAt)}
                  </td>
                  <td className={adminCellClassName}>{item.username}</td>
                  <td className={adminCellClassName}>{item.type}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(item.severity)}>{item.severity}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={item.resolved ? "success" : "warning"}>{item.resolved ? "해결됨" : "미해결"}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>
                    <Button
                      size="sm"
                      disabled={item.resolved}
                      onClick={async () => {
                        await resolveAdminIncident(item.id);
                        await load();
                      }}
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      해결
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <AdminPagination pageState={pageState} onPageChange={(page) => void load(page)} />
        </CardContent>
      </Card>
    </AdminPageShell>
  );
}
