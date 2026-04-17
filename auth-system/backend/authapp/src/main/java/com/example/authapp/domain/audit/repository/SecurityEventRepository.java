package com.example.authapp.domain.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.authapp.domain.audit.entity.SecurityEvent;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByUsernameOrderByCreatedAtDesc(String username);
    
    //********************************************************************************
    // admin 도메인
    //********************************************************************************
    
    // 보안 이벤트 조회
    List<SecurityEvent> findByUsername(String username);

    List<SecurityEvent> findByType(String type);
    

}
