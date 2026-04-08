package com.example.authapp.domain.audit.entity;

public enum SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAIL,
    LOGOUT,
    PASSWORD_CHANGE,
    TOKEN_REISSUE,
    TOKEN_THEFT_DETECTED,
    ADMIN_FORCE_LOGOUT
}
