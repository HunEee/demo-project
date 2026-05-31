package com.example.authapp.domain.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.audit.entity.AdminActionLogEntity;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLogEntity, Long> {

    List<AdminActionLogEntity> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);
    
}
