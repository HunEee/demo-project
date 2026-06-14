package com.example.authapp.domain.risk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;

public interface RiskDetectionSettingsRepository extends JpaRepository<RiskDetectionSettingsEntity, Long> {
}
