package com.example.authapp.domain.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.authapp.domain.audit.entity.SecurityEvent;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByUsernameOrderByCreatedAtDesc(String username);
    
}
