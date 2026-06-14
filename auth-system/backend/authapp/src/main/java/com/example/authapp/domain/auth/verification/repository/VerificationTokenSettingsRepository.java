package com.example.authapp.domain.auth.verification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.auth.verification.entity.VerificationTokenSettingsEntity;

public interface VerificationTokenSettingsRepository extends JpaRepository<VerificationTokenSettingsEntity, Long> {
}
