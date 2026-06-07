package com.example.authapp.domain.mfa.entity;

public enum MfaPolicy {
    OPTIONAL,
    OFF,
    REQUIRED_FOR_ADMIN,
    REQUIRED_FOR_ALL;

    public MfaPolicy normalized() {
        return this == OFF ? OPTIONAL : this;
    }
}
