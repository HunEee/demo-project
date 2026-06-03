package com.example.authapp.domain.audit.entity;

public enum SecurityIncidentType {
    TOKEN_THEFT_DETECTED("토큰 탈취 의심"),
    SUSPICIOUS_LOGIN("의심 로그인"),
    BRUTE_FORCE_ATTACK("무차별 대입"),
    IMPOSSIBLE_TRAVEL("비정상 지역 이동"),
    MFA_BYPASS_ATTEMPT("MFA 우회 시도"),
    ABNORMAL_SESSION_ACTIVITY("비정상 세션 활동");

    private final String label;

    SecurityIncidentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
