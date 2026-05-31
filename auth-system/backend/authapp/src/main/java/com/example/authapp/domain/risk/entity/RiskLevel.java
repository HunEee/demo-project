package com.example.authapp.domain.risk.entity;

public enum RiskLevel {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    CRITICAL("치명");

    private final String label;

    RiskLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
