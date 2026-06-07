import toast from "react-hot-toast";
import { Button } from "@/components/ui/button";
import type { AdminMfaUserResponse } from "@/models/MfaModels";
import { AdminConfirmDialog, AdminCrudModal, AdminFormField } from "@/pages/admin/adminUi";

export default function MfaActionDialogs({
  resetOpen,
  exceptionOpen,
  resetTarget,
  exceptionTarget,
  revokeTarget,
  selectedCount,
  resetReason,
  exceptionReason,
  exceptionExpiresAt,
  formError,
  onResetReasonChange,
  onExceptionReasonChange,
  onExceptionExpiresAtChange,
  onCloseReset,
  onCloseException,
  onCloseRevoke,
  onConfirmReset,
  onConfirmException,
  onConfirmRevoke,
}: {
  resetOpen: boolean;
  exceptionOpen: boolean;
  resetTarget: AdminMfaUserResponse | null;
  exceptionTarget: AdminMfaUserResponse | null;
  revokeTarget: AdminMfaUserResponse | null;
  selectedCount: number;
  resetReason: string;
  exceptionReason: string;
  exceptionExpiresAt: string;
  formError: string;
  onResetReasonChange: (value: string) => void;
  onExceptionReasonChange: (value: string) => void;
  onExceptionExpiresAtChange: (value: string) => void;
  onCloseReset: () => void;
  onCloseException: () => void;
  onCloseRevoke: () => void;
  onConfirmReset: () => Promise<void>;
  onConfirmException: () => Promise<void>;
  onConfirmRevoke: () => Promise<void>;
}) {
  return (
    <>
      <AdminCrudModal
        open={resetOpen}
        title={resetTarget ? "MFA 초기화" : "선택 MFA 초기화"}
        description={resetTarget ? `${resetTarget.username} 사용자의 등록된 MFA를 삭제합니다.` : `${selectedCount}개 계정의 MFA를 초기화합니다.`}
        onOpenChange={(open) => {
          if (!open) onCloseReset();
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={onCloseReset}>
              취소
            </Button>
            <Button type="button" variant="destructive" onClick={() => void onConfirmReset().catch((error: any) => toast.error(error.response?.data?.message || "MFA 초기화에 실패했습니다."))}>
              초기화
            </Button>
          </>
        }
      >
        <AdminFormField label="사유" value={resetReason} onChange={onResetReasonChange} />
      </AdminCrudModal>

      <AdminCrudModal
        open={exceptionOpen}
        title={exceptionTarget ? "MFA 예외 등록" : "선택 MFA 예외 등록"}
        description={exceptionTarget ? `${exceptionTarget.username} 사용자에게 만료일이 있는 임시 MFA 예외를 부여합니다.` : `${selectedCount}개 계정에 만료일 있는 임시 예외를 부여합니다.`}
        onOpenChange={(open) => {
          if (!open) onCloseException();
        }}
        footer={
          <>
            <Button type="button" variant="outline" onClick={onCloseException}>
              취소
            </Button>
            <Button type="button" onClick={() => void onConfirmException().catch((error: any) => toast.error(error.response?.data?.message || "MFA 예외 등록에 실패했습니다."))}>
              예외 등록
            </Button>
          </>
        }
      >
        {formError ? <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{formError}</p> : null}
        <AdminFormField label="사유" value={exceptionReason} onChange={onExceptionReasonChange} />
        <AdminFormField label="만료일시" type="datetime-local" value={exceptionExpiresAt} onChange={onExceptionExpiresAtChange} />
      </AdminCrudModal>

      <AdminConfirmDialog
        open={revokeTarget !== null}
        title="MFA 예외 해제"
        description={`${revokeTarget?.username ?? ""} 사용자의 MFA 예외를 해제합니다.`}
        confirmLabel="예외 해제"
        onOpenChange={(open) => {
          if (!open) onCloseRevoke();
        }}
        onConfirm={() => {
          void onConfirmRevoke().catch((error: any) => toast.error(error.response?.data?.message || "MFA 예외 해제에 실패했습니다."));
        }}
      />
    </>
  );
}
