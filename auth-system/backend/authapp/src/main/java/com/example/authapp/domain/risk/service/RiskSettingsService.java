package com.example.authapp.domain.risk.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.risk.entity.RiskDetectionSettingsEntity;
import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskRuleSettingsEntity;
import com.example.authapp.domain.risk.repository.RiskDetectionSettingsRepository;
import com.example.authapp.domain.risk.repository.RiskRuleSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskSettingsService {

    private final RiskDetectionSettingsRepository detectionRepository;
    private final RiskRuleSettingsRepository ruleRepository;

    @Transactional(readOnly = true)
    public RiskDetectionSettingsEntity currentDetection() {
        return detectionRepository.findById(RiskDetectionSettingsEntity.SETTINGS_ID)
                .orElseGet(RiskDetectionSettingsEntity::defaults);
    }

    @Transactional(readOnly = true)
    public Optional<RiskRuleSettingsEntity> findRule(RiskEventType eventType) {
        return ruleRepository.findByEventType(eventType);
    }

    @Transactional(readOnly = true)
    public List<RiskRuleSettingsEntity> rules() {
        return ruleRepository.findAll();
    }
}
