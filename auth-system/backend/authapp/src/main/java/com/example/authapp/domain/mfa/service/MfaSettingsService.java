package com.example.authapp.domain.mfa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.authapp.domain.mfa.entity.MfaPolicy;
import com.example.authapp.domain.mfa.entity.MfaSettingsEntity;
import com.example.authapp.domain.mfa.repository.MfaSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MfaSettingsService {

    private final MfaSettingsRepository repository;

    @Transactional(readOnly = true)
    public MfaSettingsEntity current() {
        return repository.findById(MfaSettingsEntity.SETTINGS_ID).orElseGet(MfaSettingsEntity::defaults);
    }

    @Transactional(readOnly = true)
    public MfaPolicy policy() {
        MfaPolicy policy = current().getPolicy();
        return policy == null ? MfaPolicy.OPTIONAL : policy.normalized();
    }

    @Transactional
    public MfaPolicy updatePolicy(MfaPolicy policy) {
        MfaSettingsEntity current = current();
        MfaSettingsEntity saved = repository.save(MfaSettingsEntity.builder()
                .id(MfaSettingsEntity.SETTINGS_ID)
                .policy(policy == null ? MfaPolicy.OPTIONAL : policy.normalized())
                .highRiskMfaThreshold(current.getHighRiskMfaThreshold())
                .temporaryExceptionMaxDays(current.getTemporaryExceptionMaxDays())
                .challengeFailureLimit(current.getChallengeFailureLimit())
                .challengeExpirationMinutes(current.getChallengeExpirationMinutes())
                .build());
        return saved.getPolicy();
    }
}
