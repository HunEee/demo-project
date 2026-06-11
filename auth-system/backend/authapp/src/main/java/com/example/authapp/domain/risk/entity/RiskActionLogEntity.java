package com.example.authapp.domain.risk.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "risk_action_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "risk_id")
    private Long riskId;

    @Column(name = "risk_event_id")
    private Long riskEventId;

    @Column(name = "risk_level", nullable = false, length = 30)
    private String riskLevel;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String reason;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String device;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
