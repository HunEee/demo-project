package com.example.authapp.domain.risk.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "risk_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    // 위험 점수 (0 ~ 100)
    @Column(name = "risk_score")
    private int riskScore;

    // LOW / MEDIUM / HIGH / CRITICAL
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    // 위험 이유 
    @Column(name = "risk_reson", length = 1000)
    private String riskReason;

    @Column(name = "ip_address")
    private String ipAddress;

    private String device;

    private String location;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

//    // 로그인 기록과 연결
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "login_history_id")
//    private LoginHistory loginHistory;
    
    
}
