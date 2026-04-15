package com.example.authapp.domain.audit.entity;

public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAIL,
    LOGOUT,
    PASSWORD_CHANGE,
    TOKEN_REISSUE,
    TOKEN_THEFT_DETECTED, // 토큰 탈취
    SUSPICIOUS_LOGIN,	// 의심 로그인
    ADMIN_FORCE_LOGOUT
}
