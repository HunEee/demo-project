import { useEffect, useMemo, useState } from "react";
import { Save, ShieldCheck } from "lucide-react";
import { Navigate } from "react-router";
import { hasAdminAccess } from "@/auth/permissions";
import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AdminSecurityPolicyCenter } from "@/models/AdminModels";
import AdminPageShell from "@/pages/admin/AdminPageShell";
import { AdminBadge, displayValue } from "@/pages/admin/adminUi";
import { getAdminSecurityPolicies, updateAdminSecurityPolicies } from "@/services/AdminService";

type SectionKey = "authentication" | "sessionToken" | "riskDetection" | "operationalSecurity";
type FieldValue = string | number | boolean | null | undefined;
type FieldGroup = Record<string, FieldValue>;
type SubTab = { key: string; label: string };

type SecurityPolicyCenterPageProps = {
  allowedTabs?: SectionKey[];
  title?: string;
  description?: string;
};


const subTabs: Record<SectionKey, SubTab[]> = {
  authentication: [
    { key: "login", label: "로그인" },
    { key: "password", label: "비밀번호" },
    { key: "lockout", label: "계정 잠금" },
    { key: "mfa", label: "MFA" },
    { key: "verificationToken", label: "인증/검증 토큰" },
  ],
  sessionToken: [
    { key: "tokenLifetime", label: "토큰 수명" },
    { key: "rotation", label: "Refresh Rotation" },
    { key: "sessionLimit", label: "세션 제한" },
    { key: "revocation", label: "회수 조건" },
  ],
  riskDetection: [
    { key: "thresholds", label: "탐지 기준" },
    { key: "rules", label: "위험 규칙" },
  ],
  operationalSecurity: [
    { key: "adminAccess", label: "관리자 접근" },
    { key: "corsRedirect", label: "CORS/Redirect" },
    { key: "logRetention", label: "로그 보관" },
    { key: "rateLimit", label: "Rate Limit" },
  ],
};

const numberKeys = new Set([
  "minLength",
  "historyCount",
  "expirationDays",
  "temporaryPasswordExpirationMinutes",
  "failedLoginThreshold",
  "failureWindowMinutes",
  "lockDurationMinutes",
  "adminFailedLoginThreshold",
  "highRiskMfaThreshold",
  "temporaryExceptionMaxDays",
  "challengeFailureLimit",
  "challengeExpirationMinutes",
  "signupTokenExpirationMinutes",
  "passwordResetTokenExpirationMinutes",
  "maxVerificationAttempts",
  "resendCooldownSeconds",
  "dailySendLimitPerEmail",
  "accessTokenLifetimeMinutes",
  "refreshTokenLifetimeDays",
  "rotationGraceSeconds",
  "maxActiveSessionsUser",
  "maxActiveSessionsAdmin",
  "idleTimeoutMinutes",
  "mediumThreshold",
  "highThreshold",
  "criticalThreshold",
  "mfaRequiredScore",
  "tokenRevokeScore",
  "loginBlockScore",
  "revokeAllSessionsScore",
  "adminSessionMaxAgeMinutes",
  "auditLogRetentionDays",
  "loginHistoryRetentionDays",
  "riskEventRetentionDays",
  "securityIncidentRetentionDays",
  "adminActionLogRetentionDays",
  "loginPerIpPerMinute",
  "loginPerUsernamePerMinute",
  "refreshPerSessionPerMinute",
  "passwordResetPerEmailPerDay",
  "verificationEmailPerEmailPerDay",
  "adminWritePerMinute",
]);

const labels: Record<string, string> = {
  login: "로그인 정책",
  password: "비밀번호 정책",
  lockout: "계정 잠금 정책",
  mfa: "MFA 정책",
  verificationToken: "인증/검증 토큰 정책",
  adminAccess: "관리자 접근 정책",
  corsRedirect: "CORS/Redirect 정책",
  logRetention: "로그 보관 정책",
  rateLimit: "Rate Limit 정책",
  localPasswordLoginEnabled: "로컬 비밀번호 로그인",
  socialLoginEnabled: "소셜 로그인",
  signupEnabled: "회원가입",
  rememberMeEnabled: "Remember-me",
  defaultPostLoginRoute: "로그인 후 기본 경로",
  genericLoginFailureMessageEnforced: "로그인 실패 메시지 통일",
  minLength: "최소 길이",
  requireUppercase: "대문자 필수",
  requireLowercase: "소문자 필수",
  requireDigit: "숫자 필수",
  requireSpecial: "특수문자 필수",
  historyCount: "비밀번호 이력 수",
  expirationDays: "만료 일수",
  temporaryPasswordExpirationMinutes: "임시 비밀번호 만료 분",
  blockUsernameEmailInclusion: "아이디/이메일 포함 차단",
  failedLoginThreshold: "실패 임계치",
  failureWindowMinutes: "실패 집계 분",
  lockDurationMinutes: "잠금 유지 분",
  adminFailedLoginThreshold: "관리자 실패 임계치",
  autoUnlockEnabled: "자동 잠금해제",
  manualUnlockAuditRequired: "수동 해제 감사 필수",
  policy: "MFA 정책",
  highRiskMfaThreshold: "고위험 MFA 점수",
  temporaryExceptionMaxDays: "임시 예외 최대 일",
  challengeFailureLimit: "인증 실패 제한",
  challengeExpirationMinutes: "인증 만료 분",
  signupTokenExpirationMinutes: "가입 토큰 만료 분",
  passwordResetTokenExpirationMinutes: "비밀번호 재설정 토큰 만료 분",
  maxVerificationAttempts: "검증 최대 시도",
  resendCooldownSeconds: "재전송 대기 초",
  dailySendLimitPerEmail: "이메일 일일 발송 제한",
  invalidatePreviousTokenOnResend: "재전송 시 기존 토큰 무효화",
  accessTokenLifetimeMinutes: "Access Token 수명 분",
  refreshTokenLifetimeDays: "Refresh Token 수명 일",
  rotationEnabled: "Refresh Token Rotation",
  rotationGraceSeconds: "Rotation 유예 초",
  allowRecentRotationRecovery: "최근 Rotation 복구 허용",
  maxActiveSessionsUser: "사용자 최대 세션",
  maxActiveSessionsAdmin: "관리자 최대 세션",
  idleTimeoutMinutes: "유휴 세션 만료 분",
  revokeOnPasswordChange: "비밀번호 변경 시 토큰 회수",
  revokeOnMfaReset: "MFA 초기화 시 토큰 회수",
  mediumThreshold: "MEDIUM 기준",
  highThreshold: "HIGH 기준",
  criticalThreshold: "CRITICAL 기준",
  mfaRequiredScore: "MFA 요구 점수",
  tokenRevokeScore: "토큰 회수 점수",
  loginBlockScore: "로그인 차단 점수",
  revokeAllSessionsScore: "전체 세션 회수 점수",
  autoResponseEnabled: "자동 대응",
  firstLoginNewIpExempt: "최초 로그인 신규 IP 제외",
  firstLoginNewUserAgentExempt: "최초 로그인 신규 User-Agent 제외",
  tokenReuseForceCritical: "토큰 재사용 CRITICAL 처리",
  tokenReuseRevokeAllSessions: "토큰 재사용 시 전체 세션 회수",
  tokenContextChangeRevokeFamily: "토큰 컨텍스트 변경 시 패밀리 회수",
  adminIpAllowlistEnabled: "관리자 IP allowlist 사용",
  allowedAdminIps: "허용 관리자 IP",
  denyUnknownProxyHeaders: "알 수 없는 프록시 헤더 거부",
  trustedProxyHeaderMode: "신뢰 프록시 모드",
  requireMfaForAdminAccess: "관리자 접근 MFA 필수",
  adminSessionMaxAgeMinutes: "관리자 세션 최대 분",
  allowCredentials: "Credentials 허용",
  allowedMethods: "허용 메서드",
  allowedOrigins: "허용 Origin",
  allowedRedirectUris: "허용 Redirect URI",
  auditLogRetentionDays: "감사 로그 보관 일",
  loginHistoryRetentionDays: "로그인 이력 보관 일",
  riskEventRetentionDays: "위험 이벤트 보관 일",
  securityIncidentRetentionDays: "보안 사고 보관 일",
  adminActionLogRetentionDays: "관리자 작업 로그 보관 일",
  exportRequiresReason: "내보내기 사유 필수",
  auditLogDeleteDisabled: "감사 로그 삭제 금지",
  archiveBeforePurge: "삭제 전 아카이브",
  loginPerIpPerMinute: "IP별 로그인 분당 제한",
  loginPerUsernamePerMinute: "계정별 로그인 분당 제한",
  refreshPerSessionPerMinute: "세션별 Refresh 분당 제한",
  passwordResetPerEmailPerDay: "비밀번호 재설정 일일 제한",
  verificationEmailPerEmailPerDay: "검증 이메일 일일 제한",
  adminWritePerMinute: "관리자 쓰기 분당 제한",
};

function normalizeValue(field: string, value: string | boolean) {
  if (numberKeys.has(field)) return Number(value);
  return value;
}

function updateGroup(policy: AdminSecurityPolicyCenter, section: SectionKey, group: string, field: string, value: string | boolean) {
  const sectionData = policy[section] as Record<string, unknown>;
  const currentGroup = sectionData[group] as Record<string, unknown>;
  return {
    ...policy,
    [section]: {
      ...sectionData,
      [group]: {
        ...currentGroup,
        [field]: normalizeValue(field, value),
      },
    },
  };
}

function updateFlat(policy: AdminSecurityPolicyCenter, section: "sessionToken" | "riskDetection", field: string, value: string | boolean) {
  return {
    ...policy,
    [section]: {
      ...policy[section],
      [field]: normalizeValue(field, value),
    },
  };
}

function FieldGrid({
  data,
  section,
  group,
  onChange,
}: {
  data: FieldGroup;
  section: SectionKey;
  group?: string;
  onChange: (section: SectionKey, group: string | undefined, field: string, value: string | boolean) => void;
}) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {Object.entries(data).map(([field, value]) => (
        <div key={field} className="grid gap-2">
          <Label htmlFor={`${section}-${group ?? "root"}-${field}`} className="text-xs font-semibold text-muted-foreground">
            {labels[field] ?? field}
          </Label>
          {typeof value === "boolean" ? (
            <label className="flex h-10 items-center gap-2 rounded-md border px-3 text-sm">
              <input type="checkbox" checked={value} onChange={(event) => onChange(section, group, field, event.target.checked)} />
              <span>{value ? "사용" : "미사용"}</span>
            </label>
          ) : field === "policy" ? (
            <select
              id={`${section}-${group ?? "root"}-${field}`}
              className="h-10 rounded-md border bg-background px-3 text-sm"
              value={String(value ?? "")}
              onChange={(event) => onChange(section, group, field, event.target.value)}
            >
              <option value="OFF">OFF</option>
              <option value="OPTIONAL">OPTIONAL</option>
              <option value="REQUIRED_FOR_ADMIN">REQUIRED_FOR_ADMIN</option>
              <option value="REQUIRED_FOR_ALL">REQUIRED_FOR_ALL</option>
            </select>
          ) : (
            <Input
              id={`${section}-${group ?? "root"}-${field}`}
              type={numberKeys.has(field) ? "number" : "text"}
              value={String(value ?? "")}
              onChange={(event) => onChange(section, group, field, event.target.value)}
            />
          )}
        </div>
      ))}
    </div>
  );
}

function PolicyGroups({
  section,
  groups,
  onChange,
}: {
  section: SectionKey;
  groups: Record<string, FieldGroup>;
  onChange: (section: SectionKey, group: string | undefined, field: string, value: string | boolean) => void;
}) {
  return (
    <div className="grid gap-4">
      {Object.entries(groups).map(([group, data]) => (
        <section key={group} className="rounded-md border bg-background p-4">
          <h3 className="mb-4 text-sm font-semibold">{labels[group] ?? group}</h3>
          <FieldGrid data={data} section={section} group={group} onChange={onChange} />
        </section>
      ))}
    </div>
  );
}

function SubTabBar({
  items,
  activeKey,
  onChange,
}: {
  items: SubTab[];
  activeKey: string;
  onChange: (key: string) => void;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 border-b pb-3">
      {items.map((item) => (
        <button
          key={item.key}
          type="button"
          onClick={() => onChange(item.key)}
          className={`rounded-md px-3 py-2 text-sm font-semibold transition ${
            activeKey === item.key ? "bg-foreground text-background" : "bg-muted/60 text-muted-foreground hover:text-foreground"
          }`}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}

function pickFields(data: FieldGroup, keys: string[]): FieldGroup {
  return Object.fromEntries(keys.map((key) => [key, data[key]]));
}

export default function SecurityPolicyCenterPage({
  allowedTabs,
  title = "정책 관리",
  description = "인증, 세션/토큰, 위험탐지, 운영 보안 정책을 관리합니다.",
}: SecurityPolicyCenterPageProps) {
  const user = useAuth((state) => state.user);
  const isAdmin = hasAdminAccess(user);
  const activeTab: SectionKey = allowedTabs?.[0] ?? "authentication";
  const [activeSubTab, setActiveSubTab] = useState<string>(subTabs[allowedTabs?.[0] ?? "authentication"][0].key);
  const [policy, setPolicy] = useState<AdminSecurityPolicyCenter | null>(null);
  const [savedPolicy, setSavedPolicy] = useState<AdminSecurityPolicyCenter | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const changed = useMemo(() => JSON.stringify(policy) !== JSON.stringify(savedPolicy), [policy, savedPolicy]);

  useEffect(() => {
    if (!isAdmin) return;
    setLoading(true);
    void getAdminSecurityPolicies()
      .then((data) => {
        setPolicy(data);
        setSavedPolicy(data);
        setError(null);
      })
      .catch(() => setError("보안 정책을 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [isAdmin]);

  if (!isAdmin) return <Navigate to="/dashboard" replace />;

  const handleChange = (section: SectionKey, group: string | undefined, field: string, value: string | boolean) => {
    setPolicy((current) => {
      if (!current) return current;
      if (section === "sessionToken" || section === "riskDetection") return updateFlat(current, section, field, value);
      if (!group) return current;
      return updateGroup(current, section, group, field, value);
    });
  };

  const handleRuleChange = (index: number, field: string, value: string | boolean) => {
    setPolicy((current) => {
      if (!current) return current;
      const nextRules = current.riskRules.map((rule, ruleIndex) => {
        if (ruleIndex !== index) return rule;
        return {
          ...rule,
          [field]: field === "score" || field.endsWith("Hour") || field.endsWith("Minutes") || field.endsWith("Count")
            ? value === "" ? null : Number(value)
            : value,
        };
      });
      return { ...current, riskRules: nextRules };
    });
  };

  const handleSave = async () => {
    if (!policy) return;
    setLoading(true);
    try {
      const saved = await updateAdminSecurityPolicies(policy);
      setPolicy(saved);
      setSavedPolicy(saved);
      setError(null);
    } catch {
      setError("보안 정책 저장에 실패했습니다. 입력값을 확인하세요.");
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
        <ShieldCheck className="h-4 w-4 text-muted-foreground" />
        <span className="text-muted-foreground">변경 상태</span>
        <AdminBadge tone={changed ? "warning" : "success"}>{changed ? "저장 필요" : "저장됨"}</AdminBadge>
        {error ? <span className="text-destructive">{error}</span> : null}
      </div>

      {loading && !policy ? <p className="text-sm text-muted-foreground">불러오는 중입니다.</p> : null}

      {policy && activeTab === "authentication" ? (
        <div className="grid gap-4">
          <SubTabBar items={subTabs.authentication} activeKey={activeSubTab} onChange={setActiveSubTab} />
          <PolicyGroups
            section="authentication"
            groups={{ [activeSubTab]: (policy.authentication as unknown as Record<string, FieldGroup>)[activeSubTab] }}
            onChange={handleChange}
          />
        </div>
      ) : null}

      {policy && activeTab === "sessionToken" ? (
        <div className="grid gap-4">
          <SubTabBar items={subTabs.sessionToken} activeKey={activeSubTab} onChange={setActiveSubTab} />
          <section className="rounded-md border bg-background p-4">
            <FieldGrid
              data={sessionTokenGroup(policy.sessionToken as unknown as FieldGroup, activeSubTab)}
              section="sessionToken"
              onChange={handleChange}
            />
          </section>
        </div>
      ) : null}

      {policy && activeTab === "riskDetection" ? (
        <div className="grid gap-4">
          <SubTabBar items={subTabs.riskDetection} activeKey={activeSubTab} onChange={setActiveSubTab} />
          {activeSubTab === "thresholds" ? (
            <section className="rounded-md border bg-background p-4">
              <FieldGrid data={policy.riskDetection as unknown as FieldGroup} section="riskDetection" onChange={handleChange} />
            </section>
          ) : (
            <RiskRuleTable rules={policy.riskRules} onChange={handleRuleChange} />
          )}
        </div>
      ) : null}

      {policy && activeTab === "operationalSecurity" ? (
        <div className="grid gap-4">
          <SubTabBar items={subTabs.operationalSecurity} activeKey={activeSubTab} onChange={setActiveSubTab} />
          <PolicyGroups
            section="operationalSecurity"
            groups={{ [activeSubTab]: (policy.operationalSecurity as unknown as Record<string, FieldGroup>)[activeSubTab] }}
            onChange={handleChange}
          />
        </div>
      ) : null}
    </AdminPageShell>
  );
}

function sessionTokenGroup(data: FieldGroup, group: string): FieldGroup {
  if (group === "tokenLifetime") {
    return pickFields(data, ["accessTokenLifetimeMinutes", "refreshTokenLifetimeDays", "idleTimeoutMinutes"]);
  }
  if (group === "rotation") {
    return pickFields(data, ["rotationEnabled", "rotationGraceSeconds", "allowRecentRotationRecovery"]);
  }
  if (group === "sessionLimit") {
    return pickFields(data, ["maxActiveSessionsUser", "maxActiveSessionsAdmin"]);
  }
  return pickFields(data, ["revokeOnPasswordChange", "revokeOnMfaReset"]);
}

function RiskRuleTable({
  rules,
  onChange,
}: {
  rules: AdminSecurityPolicyCenter["riskRules"];
  onChange: (index: number, field: string, value: string | boolean) => void;
}) {
  return (
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full min-w-[1080px] text-sm">
        <thead className="bg-muted/70">
          <tr>
            <th className="px-3 py-2 text-left">이벤트</th>
            <th className="px-3 py-2 text-center">사용</th>
            <th className="px-3 py-2 text-center">점수</th>
            <th className="px-3 py-2 text-center">위험도</th>
            <th className="px-3 py-2 text-center">횟수</th>
            <th className="px-3 py-2 text-center">기간</th>
            <th className="px-3 py-2 text-center">야간 시작</th>
            <th className="px-3 py-2 text-center">야간 종료</th>
            <th className="px-3 py-2 text-left">설명</th>
          </tr>
        </thead>
        <tbody>
          {rules.map((rule, index) => (
            <tr key={rule.eventType} className="border-t">
              <td className="px-3 py-2 font-medium">{displayValue(rule.eventType)}</td>
              <td className="px-3 py-2 text-center">
                <input type="checkbox" checked={rule.enabled} onChange={(event) => onChange(index, "enabled", event.target.checked)} />
              </td>
              <td className="px-3 py-2">
                <Input type="number" value={rule.score} onChange={(event) => onChange(index, "score", event.target.value)} />
              </td>
              <td className="px-3 py-2">
                <select
                  className="h-10 w-full rounded-md border bg-background px-3"
                  value={rule.riskLevel}
                  onChange={(event) => onChange(index, "riskLevel", event.target.value)}
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </select>
              </td>
              <td className="px-3 py-2">
                <Input type="number" value={rule.thresholdCount ?? ""} onChange={(event) => onChange(index, "thresholdCount", event.target.value)} />
              </td>
              <td className="px-3 py-2">
                <Input type="number" value={rule.windowMinutes ?? ""} onChange={(event) => onChange(index, "windowMinutes", event.target.value)} />
              </td>
              <td className="px-3 py-2">
                <Input type="number" value={rule.nightStartHour ?? ""} onChange={(event) => onChange(index, "nightStartHour", event.target.value)} />
              </td>
              <td className="px-3 py-2">
                <Input type="number" value={rule.nightEndHour ?? ""} onChange={(event) => onChange(index, "nightEndHour", event.target.value)} />
              </td>
              <td className="px-3 py-2">
                <Input value={rule.description ?? ""} onChange={(event) => onChange(index, "description", event.target.value)} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
