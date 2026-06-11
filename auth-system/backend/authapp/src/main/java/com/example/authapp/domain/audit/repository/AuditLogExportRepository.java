package com.example.authapp.domain.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.audit.entity.AuditLogExportEntity;

public interface AuditLogExportRepository extends JpaRepository<AuditLogExportEntity, Long> {
}
