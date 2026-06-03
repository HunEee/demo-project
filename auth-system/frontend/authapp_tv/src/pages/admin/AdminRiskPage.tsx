import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import useAuth from "@/auth/store";
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
import type { AdminFilterOptions, AdminRisk } from "@/models/AdminModels";
import { getAdminFilterOptions, getAdminRisks } from "@/services/AdminService";

export default function AdminRiskPage() {
  const user = useAuth((state) => state.user);
  const [items, setItems] = useState<AdminRisk[]>([]);
  const [filters, setFilters] = useState({ username: "", level: "", minScore: "" });
  const [filterOptions, setFilterOptions] = useState<AdminFilterOptions | null>(null);
  const [pageState, setPageState] = useState<PageState>({ page: 0, size: 10, totalPages: 1, totalElements: 0 });
  const [sortState, setSortState] = useState<SortState>({ sort: "riskScore", direction: "DESC" });
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

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
    const page = await getAdminRisks({ username: nextFilters.username, level: nextFilters.level, minScore: nextFilters.minScore, page: 0, size: pageState.size, sort: sortState.sort, direction: sortState.direction });
    setItems(page.content);
    setPageState((prev) => ({ ...prev, page: page.page, size: page.size, totalPages: page.totalPages, totalElements: page.totalElements }));
  };

  useEffect(() => {
    if (isAdmin) {
      void load().catch(() => undefined);
      void getAdminFilterOptions().then(setFilterOptions).catch(() => undefined);
    }
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  return (
    <AdminPageShell title="위험 사용자 관리" description="위험 점수와 최근 판단 사유를 확인합니다.">
      <AdminFilters
        fields={[
          { name: "username", label: "사용자 검색", placeholder: "아이디" },
          {
            name: "level",
            label: "위험 레벨",
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

      <Card className="rounded-lg">
        <CardContent className="overflow-x-auto p-0">
          <table className={adminTableClassName}>
            <thead className={adminTheadClassName}>
              <tr>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="점수" column="riskScore" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="레벨" column="riskLevel" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="최근 사유" column="lastReason" sortState={sortState} onSort={handleSort} />
                </th>
                <th className={adminCellClassName}>
                  <AdminSortableHeader label="갱신" column="updatedAt" sortState={sortState} onSort={handleSort} />
                </th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? <AdminEmptyRow colSpan={5} /> : null}
              {items.map((item) => (
                <tr key={item.id} className={adminRowClassName}>
                  <td className={adminCellClassName}>{item.username}</td>
                  <td className={`${adminCellClassName} tabular-nums`}>{item.riskScore}</td>
                  <td className={adminCellClassName}>
                    <AdminBadge tone={statusTone(item.riskLevel)}>{item.riskLevel}</AdminBadge>
                  </td>
                  <td className={adminCellClassName}>{item.lastReason || "-"}</td>
                  <td className={`${adminCellClassName} whitespace-nowrap tabular-nums`}>
                    {formatSecurityDateTime(item.updatedAt)}
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
