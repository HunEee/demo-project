package com.example.authapp.domain.profile.entity;

public enum UserProfileStatus {
    ACTIVE("활성"),
    LOCKED("잠금"),
    DISABLED("비활성"),
    EXPIRED("만료"),
    LEAVE("휴직"),
    DELETED("탈퇴");

    private final String label;

    UserProfileStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
