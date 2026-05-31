package com.example.authapp.domain.admin.type;

public enum AdminUserStatus {
    ACTIVE("활성"),
    LOCKED("잠금"),
    DISABLED("비활성"),
    DELETED("탈퇴");

    private final String label;

    AdminUserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
}
