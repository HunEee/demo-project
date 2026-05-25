import type { ReactNode } from "react";
import { ArrowDown, ArrowUp, ChevronsUpDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const toneClassNames = {
  default: "border-border bg-muted/60 text-muted-foreground",
  success: "border-emerald-200 bg-emerald-50 text-emerald-700",
  warning: "border-amber-200 bg-amber-50 text-amber-700",
  danger: "border-red-200 bg-red-50 text-red-700",
  info: "border-sky-200 bg-sky-50 text-sky-700",
} as const;

type Tone = keyof typeof toneClassNames;
export type SortDirection = "ASC" | "DESC";
export type SortState = { sort: string; direction: SortDirection };
export type PageState = { page: number; size: number; totalPages: number; totalElements: number };

export function AdminBadge({ children, tone = "default" }: { children: ReactNode; tone?: Tone }) {
  return (
    <span className={cn("inline-flex rounded-full border px-2 py-0.5 text-xs font-medium", toneClassNames[tone])}>
      {children}
    </span>
  );
}

export function AdminEmptyRow({ colSpan, message = "표시할 데이터가 없습니다." }: { colSpan: number; message?: string }) {
  return (
    <tr>
      <td className="px-5 py-10 text-center text-sm text-muted-foreground" colSpan={colSpan}>
        {message}
      </td>
    </tr>
  );
}

export function AdminSortableHeader({
  label,
  column,
  sortState,
  onSort,
}: {
  label: string;
  column: string;
  sortState: SortState;
  onSort: (column: string) => void;
}) {
  const active = sortState.sort === column;
  const Icon = active ? (sortState.direction === "ASC" ? ArrowUp : ArrowDown) : ChevronsUpDown;

  return (
    <button
      type="button"
      className={cn(
        "mx-auto inline-flex h-8 items-center justify-center gap-1 rounded-lg px-2 text-xs font-medium transition hover:bg-background",
        active && "text-foreground",
      )}
      onClick={() => onSort(column)}
    >
      {label}
      <Icon className="h-3.5 w-3.5" />
    </button>
  );
}

export function AdminPagination({
  pageState,
  onPageChange,
}: {
  pageState: PageState;
  onPageChange: (page: number) => void;
}) {
  const currentPage = pageState.page + 1;
  const totalPages = Math.max(pageState.totalPages, 1);
  const start = pageState.totalElements === 0 ? 0 : pageState.page * pageState.size + 1;
  const end = Math.min((pageState.page + 1) * pageState.size, pageState.totalElements);

  return (
    <div className="flex flex-col gap-3 border-t bg-muted/20 px-5 py-4 text-sm sm:flex-row sm:items-center sm:justify-between">
      <p className="text-muted-foreground">
        {start}-{end} / 총 {pageState.totalElements}건
      </p>
      <div className="flex items-center justify-end gap-2">
        <Button variant="outline" size="sm" disabled={pageState.page <= 0} onClick={() => onPageChange(pageState.page - 1)}>
          이전
        </Button>
        <span className="min-w-20 text-center tabular-nums">
          {currentPage} / {totalPages}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange(pageState.page + 1)}
        >
          다음
        </Button>
      </div>
    </div>
  );
}

export const adminTableClassName = "w-full min-w-[860px] text-sm";
export const adminTheadClassName = "border-b bg-muted/40 text-xs uppercase text-muted-foreground";
export const adminRowClassName = "border-b transition-colors hover:bg-muted/30";
export const adminCellClassName = "px-5 py-3 align-middle text-center";

export const statusTone = (value?: string | boolean | null): Tone => {
  if (value === true) return "success";
  const text = String(value ?? "").toUpperCase();
  if (["SUCCESS", "ACTIVE", "LOW", "RESOLVED", "활성", "해결"].includes(text)) return "success";
  if (["MEDIUM", "WARNING", "WARN"].includes(text)) return "warning";
  if (["HIGH", "CRITICAL", "FAILED", "FAIL", "LOCKED", "REVOKED", "DELETED", "잠금", "폐기", "탈퇴"].includes(text)) return "danger";
  return "default";
};
