package com.example.authapp.domain.mfa.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.mfa.entity.MfaMethodEntity;
import com.example.authapp.domain.mfa.entity.MfaMethodType;

public record MfaMethodResponse(
        Long id,
        MfaMethodType type,
        boolean enabled,
        LocalDateTime registeredAt,
        LocalDateTime lastUsedAt
) {
    public static MfaMethodResponse from(MfaMethodEntity entity) {
        return new MfaMethodResponse(
                entity.getId(),
                entity.getType(),
                entity.isEnabled(),
                entity.getRegisteredAt(),
                entity.getLastUsedAt()
        );
    }
}
