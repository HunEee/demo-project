package com.example.authapp.domain.risk.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.authapp.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "risk_entity")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private int riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private String lastReason;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    //*********************************************************
    // 연관관계
    //********************************************************* 
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;
    
    //*********************************************************
    // 커스텀 메서드
    //*********************************************************    
    
    // 점수 누적 메서드
    public void increaseRisk(int score, String reason) {
        this.riskScore = Math.min(100, this.riskScore + score);
        this.riskLevel = calculateLevel(this.riskScore);
        this.lastReason = reason;
    }
    
    // 리스크 레벨 판별
    private RiskLevel calculateLevel(int score) {
        if (score >= 80) return RiskLevel.CRITICAL;
        if (score >= 60) return RiskLevel.HIGH;
        if (score >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
    
    public void forceCritical(String reason) {
        this.riskScore = 100;
        this.riskLevel = RiskLevel.CRITICAL;
        this.lastReason = reason;
    }
    
}
