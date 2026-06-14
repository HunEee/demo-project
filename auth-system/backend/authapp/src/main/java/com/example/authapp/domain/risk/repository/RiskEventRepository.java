package com.example.authapp.domain.risk.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.authapp.domain.risk.entity.RiskEventType;
import com.example.authapp.domain.risk.entity.RiskEventEntity;
import com.example.authapp.domain.risk.entity.RiskLevel;

public interface RiskEventRepository extends JpaRepository<RiskEventEntity, Long> {

    @Query("""
            select event
            from RiskEventEntity event
            where (:username is null or lower(event.username) like lower(concat('%', :username, '%')))
              and (:eventType is null or event.eventType = :eventType)
              and (:riskLevel is null or event.riskLevel = :riskLevel)
              and (:resolved is null or event.resolved = :resolved)
              and (:from is null or event.createdAt >= :from)
              and (:to is null or event.createdAt <= :to)
            """)
    Page<RiskEventEntity> search(
            @Param("username") String username,
            @Param("eventType") RiskEventType eventType,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("resolved") Boolean resolved,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    long countByResolvedFalse();

    long countByRiskLevelAndCreatedAtAfter(RiskLevel riskLevel, LocalDateTime createdAt);
}
