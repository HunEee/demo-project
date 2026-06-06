package com.example.authapp.domain.hr.entity;

public enum HrUserStatus {
    ACTIVE("Active"),
    LEAVE("Leave"),
    RETIRED("Retired"),
    SUSPENDED("Suspended");

    private final String label;

    HrUserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
