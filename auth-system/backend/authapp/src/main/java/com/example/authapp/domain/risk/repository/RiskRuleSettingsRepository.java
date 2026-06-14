package com.example.authapp.domain.risk.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskRuleSettingsEntity;

public interface RiskRuleSettingsRepository extends JpaRepository<RiskRuleSettingsEntity, Long> {
    Optional<RiskRuleSettingsEntity> findByEventType(RiskEventType eventType);
}
