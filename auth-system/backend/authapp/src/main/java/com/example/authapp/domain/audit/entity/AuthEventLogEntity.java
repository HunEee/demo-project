package com.example.authapp.domain.audit.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
	    name = "auth_event_log",
	    indexes = {
	        @Index(name = "idx_auth_event_username", columnList = "username"),
	        @Index(name = "idx_auth_event_created_at", columnList = "created_at"),
	        @Index(name = "idx_auth_event_type", columnList = "type")
	    }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEventLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    // 이벤트 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private AuthEventType type;

    // 상세 메시지
    @Column(name = "description", length = 500)
    private String description;

    // IP
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // 디바이스
    @Column(name = "device", length = 100)
    private String device;

    // 시간
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
}
