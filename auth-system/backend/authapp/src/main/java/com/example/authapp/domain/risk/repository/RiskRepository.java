package com.example.authapp.domain.risk.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.authapp.domain.risk.entity.RiskEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;

public interface RiskRepository extends JpaRepository<RiskEntity, Long> {

    List<RiskEntity> findByUsername(String username);

    List<RiskEntity> findByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT r FROM RiskEntity r ORDER BY r.createdAt DESC")
    List<RiskEntity> findRecentRisks(Pageable pageable);
    
}
