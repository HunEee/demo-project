package com.example.authapp.domain.audit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.authapp.domain.audit.entity.SecurityIncidentEntity;
import com.example.authapp.domain.audit.entity.SecurityIncidentType;
import com.example.authapp.domain.audit.entity.Severity;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncidentEntity, Long> {
	
    // 미해결 사건
    List<SecurityIncidentEntity> findByResolvedFalseOrderByCreatedAtDesc();

    // 특정 사용자 미해결 사건
    List<SecurityIncidentEntity> findByUsernameAndResolvedFalseOrderByCreatedAtDesc(String username);

    // 사용자 전체 사건
    List<SecurityIncidentEntity> findByUsernameOrderByCreatedAtDesc(String username);

    // 유형별 조회
    List<SecurityIncidentEntity> findByTypeOrderByCreatedAtDesc(SecurityIncidentType type);

    // 심각도별 미해결 조회
    List<SecurityIncidentEntity> findBySeverityAndResolvedFalseOrderByCreatedAtDesc(Severity severity);

    // 사용자 미해결 건수
    long countByUsernameAndResolvedFalse(String username);

    // 기간 내 생성 건수
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
	
}

