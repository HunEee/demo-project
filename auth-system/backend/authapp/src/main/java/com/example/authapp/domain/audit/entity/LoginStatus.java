package com.example.authapp.domain.audit.entity;

public enum LoginStatus {
    SUCCESS("성공"),
    FAILED("실패"),
    LOGOUT("로그아웃"),
    EXPIRED("만료");

    private final String label;

    LoginStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
}
