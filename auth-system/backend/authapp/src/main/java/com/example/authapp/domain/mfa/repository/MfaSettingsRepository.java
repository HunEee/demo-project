package com.example.authapp.domain.mfa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.mfa.entity.MfaSettingsEntity;

public interface MfaSettingsRepository extends JpaRepository<MfaSettingsEntity, Long> {
}
