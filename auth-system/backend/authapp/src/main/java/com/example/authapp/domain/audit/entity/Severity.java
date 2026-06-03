package com.example.authapp.domain.audit.entity;

public enum Severity {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    CRITICAL("치명");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
