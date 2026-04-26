package com.example.authapp.domain.risk.service;

import org.springframework.stereotype.Component;

import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;
import com.example.authapp.domain.user.entity.UserEntity;

@Component
public class RiskFactory {

    public RiskEntity create(UserEntity user) {
        return RiskEntity.builder()
                .user(user)
                .username(user.getUsername())
                .riskScore(0)
                .riskLevel(RiskLevel.LOW)
                .lastReason("INIT")
                .build();
    }
    
}
