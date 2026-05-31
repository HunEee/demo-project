package com.example.authapp.domain.audit.entity;

public enum AuthEventType {
    SIGNUP_SUCCESS("일반 회원가입"),
    SIGNUP_OAUTH2_SUCCESS("OAuth2 회원가입"),

    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAIL("로그인 실패"),
    LOGOUT("로그아웃"),

    PASSWORD_CHANGE("비밀번호 변경"),
    PASSWORD_RESET("비밀번호 초기화"),

    TOKEN_REISSUE("토큰 재발급"),

    ADMIN_FORCE_LOGOUT("관리자 강제 로그아웃"),
    SECURITY_FORCE_LOGOUT("보안 강제 로그아웃"),

    ACCOUNT_PROFILE_UPDATED("프로필 수정"),
    ACCOUNT_DEACTIVATED("계정 비활성화"),
    ACCOUNT_DELETE("계정 삭제");

    private final String label;

    AuthEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
}
