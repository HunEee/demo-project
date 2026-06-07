import { Button } from "@/components/ui/button";
import type { MfaPolicy } from "@/models/MfaModels";

const policyOptions: Array<{ label: string; value: MfaPolicy }> = [
  { label: "사용자 자율", value: "OPTIONAL" },
  { label: "관리자 미등록 차단", value: "REQUIRED_FOR_ADMIN" },
  { label: "전체 미등록 차단", value: "REQUIRED_FOR_ALL" },
];

export default function MfaPolicyToolbar({
  policy,
  onPolicyChange,
  onSave,
}: {
  policy: MfaPolicy;
  onPolicyChange: (policy: MfaPolicy) => void;
  onSave: () => void;
}) {
  return (
    <>
      <select
        className="h-9 rounded-lg border border-input bg-background px-3 text-sm"
        value={policy === "OFF" ? "OPTIONAL" : policy}
        onChange={(event) => onPolicyChange(event.target.value as MfaPolicy)}
      >
        {policyOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <Button type="button" onClick={onSave}>
        정책 저장
      </Button>
    </>
  );
}
