package com.example.authapp.domain.audit.entity;

public enum AuthEventType {
    SIGNUP_SUCCESS,
    SIGNUP_OAUTH2_SUCCESS,
    LOGIN_SUCCESS,
    LOGIN_FAIL,
    LOGOUT,					// 사용자 로그아웃
    PASSWORD_CHANGE,
    PASSWORD_RESET,
    TOKEN_REISSUE,
    ADMIN_FORCE_LOGOUT,     // 관리자 수동 조치
    SECURITY_FORCE_LOGOUT,  // 시스템 자동 차단
    ACCOUNT_DELETE
}
