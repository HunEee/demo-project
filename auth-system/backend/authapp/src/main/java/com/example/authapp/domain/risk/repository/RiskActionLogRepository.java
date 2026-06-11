package com.example.authapp.domain.risk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.risk.entity.RiskActionLogEntity;

public interface RiskActionLogRepository extends JpaRepository<RiskActionLogEntity, Long> {

    List<RiskActionLogEntity> findByUsernameOrderByCreatedAtDesc(String username);
}
