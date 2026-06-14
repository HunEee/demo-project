package com.example.authapp.domain.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.audit.entity.AuditSettingsEntity;

public interface AuditSettingsRepository extends JpaRepository<AuditSettingsEntity, Long> {
}
