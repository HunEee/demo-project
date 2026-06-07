package com.example.authapp.domain.mfa.dto;

import java.util.List;

import com.example.authapp.domain.mfa.entity.MfaMethodType;

public record MfaLoginDecision(
        boolean required,
        boolean registrationRequired,
        List<MfaMethodType> availableMethods
) {
    public static MfaLoginDecision notRequired() {
        return new MfaLoginDecision(false, false, List.of());
    }

    public static MfaLoginDecision requireRegistration() {
        return new MfaLoginDecision(true, true, List.of());
    }
}
