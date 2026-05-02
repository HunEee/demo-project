package com.example.authapp.domain.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.authapp.domain.audit.entity.AuthEventLogEntity;
import com.example.authapp.domain.audit.entity.AuthEventType;

import java.time.LocalDateTime;
import java.util.List;

public interface AuthEventLogRepository extends JpaRepository<AuthEventLogEntity, Long> {

	// 사용자 이벤트 로그 최신순
    List<AuthEventLogEntity> findByUsernameOrderByCreatedAtDesc(String username);
    
    // 특정 이벤트 타입 조회
    List<AuthEventLogEntity> findByUsernameAndTypeOrderByCreatedAtDesc(String username, AuthEventType type);

    // 기간 조회
    List<AuthEventLogEntity> findByUsernameAndCreatedAtBetweenOrderByCreatedAtDesc(String username, LocalDateTime from, LocalDateTime to);
    
    
    //********************************************************************************
    // admin 도메인
    //********************************************************************************
    
    // 보안 이벤트 조회
    List<AuthEventLogEntity> findByUsername(String username);

    List<AuthEventLogEntity> findByType(String type);
    

}
