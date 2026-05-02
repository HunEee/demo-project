package com.example.authapp.domain.audit.entity;

public enum SecurityIncidentType {
    TOKEN_THEFT_DETECTED,
    SUSPICIOUS_LOGIN,
    BRUTE_FORCE_ATTACK,
    IMPOSSIBLE_TRAVEL,
    MFA_BYPASS_ATTEMPT,
    ABNORMAL_SESSION_ACTIVITY
}
