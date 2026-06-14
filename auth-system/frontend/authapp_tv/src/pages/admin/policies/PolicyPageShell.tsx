import { useEffect, useMemo, useState } from "react";
import type { Dispatch, ReactNode, SetStateAction } from "react";
import { Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { AdminSecurityPolicyCenter } from "@/models/AdminModels";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { AdminBadge } from "@/pages/admin/adminUi";
import { getAdminSecurityPolicies, updateAdminSecurityPolicies } from "@/services/AdminService";

type Props = {
  title: string;
  description: string;
  children: (args: {
    policy: AdminSecurityPolicyCenter;
    setPolicy: Dispatch<SetStateAction<AdminSecurityPolicyCenter | null>>;
  }) => ReactNode;
};

export default function PolicyPageShell({ title, description, children }: Props) {
  const [policy, setPolicy] = useState<AdminSecurityPolicyCenter | null>(null);
  const [savedPolicy, setSavedPolicy] = useState<AdminSecurityPolicyCenter | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const changed = useMemo(() => JSON.stringify(policy) !== JSON.stringify(savedPolicy), [policy, savedPolicy]);

  useEffect(() => {
    setLoading(true);
    void getAdminSecurityPolicies()
      .then((data) => {
        setPolicy(data);
        setSavedPolicy(data);
        setError(null);
      })
      .catch(() => setError("정책 정보를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (!policy) return;
    setLoading(true);
    try {
      const saved = await updateAdminSecurityPolicies(policy);
      setPolicy(saved);
      setSavedPolicy(saved);
      setError(null);
    } catch {
      setError("정책 저장에 실패했습니다. 입력값을 확인하세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AdminPageShell
      title={title}
      description={description}
      actions={
        <Button type="button" className="h-9" disabled={!changed || loading || !policy} onClick={() => void handleSave()}>
          <Save className="h-4 w-4" />
          저장
        </Button>
      }
    >
      <div className="flex items-center gap-2 text-sm">
        <span className="text-muted-foreground">변경 상태</span>
        <AdminBadge tone={changed ? "warning" : "success"}>{changed ? "저장 필요" : "저장됨"}</AdminBadge>
        {error ? <span className="text-destructive">{error}</span> : null}
      </div>
      {loading && !policy ? <p className="text-sm text-muted-foreground">불러오는 중입니다.</p> : null}
      {policy ? children({ policy, setPolicy }) : null}
    </AdminPageShell>
  );
}
