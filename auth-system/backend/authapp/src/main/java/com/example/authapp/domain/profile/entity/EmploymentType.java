package com.example.authapp.domain.profile.entity;

public enum EmploymentType {
    EMPLOYEE("정규직"),
    CONTRACTOR("계약직"),
    EXTERNAL("외부"),
    UNKNOWN("미지정");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
}
