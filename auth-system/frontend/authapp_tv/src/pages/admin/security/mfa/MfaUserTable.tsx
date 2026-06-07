import { RotateCcw, ShieldCheck, ShieldOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { AdminMfaUserResponse } from "@/models/MfaModels";
import {
  AdminBadge,
  AdminEmptyRow,
  AdminPagination,
  AdminSortableHeader,
  AdminTableCard,
  adminCellClassName,
  adminRowClassName,
  adminTableClassName,
  adminTheadClassName,
  type PageState,
  type SortState,
} from "@/pages/admin/adminUi";
import MfaDateTimeCell from "@/pages/admin/security/mfa/MfaDateTimeCell";

type SelectionState = {
  allPageSelected: boolean;
  isSelected: (id: string) => boolean;
  togglePage: (checked: boolean) => void;
  toggleItem: (id: string, checked: boolean) => void;
};

export default function MfaUserTable({
  loading,
  users,
  sortState,
  onSort,
  selection,
  pageState,
  onPageChange,
  onReset,
  onException,
  onRevokeException,
}: {
  loading: boolean;
  users: AdminMfaUserResponse[];
  sortState: SortState;
  onSort: (column: string) => void;
  selection: SelectionState;
  pageState: PageState;
  onPageChange: (page: number) => void;
  onReset: (user: AdminMfaUserResponse) => void;
  onException: (user: AdminMfaUserResponse) => void;
  onRevokeException: (user: AdminMfaUserResponse) => void;
}) {
  return (
    <AdminTableCard>
      <table className={adminTableClassName}>
        <thead className={adminTheadClassName}>
          <tr>
            <th className={adminCellClassName}>
              <input
                type="checkbox"
                aria-label="현재 페이지 사용자 전체 선택"
                checked={selection.allPageSelected}
                onChange={(event) => selection.togglePage(event.target.checked)}
              />
            </th>
            <th className={adminCellClassName}>
              <AdminSortableHeader label="사용자" column="username" sortState={sortState} onSort={onSort} />
            </th>
            <th className={adminCellClassName}>
              <AdminSortableHeader label="MFA" column="mfaEnabled" sortState={sortState} onSort={onSort} />
            </th>
            <th className={adminCellClassName}>방식</th>
            <th className={adminCellClassName}>등록일</th>
            <th className={adminCellClassName}>
              <AdminSortableHeader label="마지막 사용" column="lastUsedAt" sortState={sortState} onSort={onSort} />
            </th>
            <th className={adminCellClassName}>정책</th>
            <th className={adminCellClassName}>
              <AdminSortableHeader label="예외" column="exceptionExpiresAt" sortState={sortState} onSort={onSort} />
            </th>
            <th className={adminCellClassName}>작업</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <AdminEmptyRow colSpan={9} message="불러오는 중..." />
          ) : users.length === 0 ? (
            <AdminEmptyRow colSpan={9} />
          ) : (
            users.map((user) => (
              <tr key={user.username} className={adminRowClassName}>
                <td className={adminCellClassName}>
                  <input
                    type="checkbox"
                    aria-label={`${user.username} 선택`}
                    checked={selection.isSelected(user.username)}
                    onChange={(event) => selection.toggleItem(user.username, event.target.checked)}
                  />
                </td>
                <td className={adminCellClassName}>
                  <div className="text-center">
                    <p className="font-medium">{user.username}</p>
                    <p className="text-xs text-muted-foreground">{user.email ?? "-"}</p>
                  </div>
                </td>
                <td className={adminCellClassName}>
                  <AdminBadge tone={user.mfaEnabled ? "success" : "warning"}>{user.mfaEnabled ? "등록" : "미등록"}</AdminBadge>
                </td>
                <td className={adminCellClassName}>{user.method ?? "-"}</td>
                <td className={adminCellClassName}>
                  <MfaDateTimeCell value={user.registeredAt} />
                </td>
                <td className={adminCellClassName}>
                  <MfaDateTimeCell value={user.lastUsedAt} />
                </td>
                <td className={adminCellClassName}>
                  {user.requiredByPolicy ? <AdminBadge tone="info">대상</AdminBadge> : "-"}
                </td>
                <td className={adminCellClassName}>
                  {user.exceptionActive ? (
                    <AdminBadge tone="info">
                      <MfaDateTimeCell value={user.exceptionExpiresAt} />
                    </AdminBadge>
                  ) : (
                    "-"
                  )}
                </td>
                <td className={adminCellClassName}>
                  <div className="flex flex-wrap justify-center gap-2">
                    <Button type="button" variant="outline" size="sm" disabled={!user.mfaEnabled} onClick={() => onReset(user)}>
                      <RotateCcw className="h-4 w-4" />
                      초기화
                    </Button>
                    {user.exceptionActive ? (
                      <Button type="button" variant="outline" size="sm" onClick={() => onRevokeException(user)}>
                        <ShieldCheck className="h-4 w-4" />
                        예외 해제
                      </Button>
                    ) : (
                      <Button type="button" variant="outline" size="sm" onClick={() => onException(user)}>
                        <ShieldOff className="h-4 w-4" />
                        예외
                      </Button>
                    )}
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      <AdminPagination pageState={pageState} onPageChange={onPageChange} />
    </AdminTableCard>
  );
}
