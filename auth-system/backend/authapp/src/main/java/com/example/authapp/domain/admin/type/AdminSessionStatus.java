package com.example.authapp.domain.admin.type;

public enum AdminSessionStatus {
    ACTIVE("활성"),
    REVOKED("폐기");

    private final String label;

    AdminSessionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
}
