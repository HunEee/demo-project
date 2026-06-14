package com.example.authapp.domain.risk.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "risk_detection_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskDetectionSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "medium_threshold", nullable = false)
    private int mediumThreshold;

    @Column(name = "high_threshold", nullable = false)
    private int highThreshold;

    @Column(name = "critical_threshold", nullable = false)
    private int criticalThreshold;

    @Column(name = "mfa_required_score", nullable = false)
    private int mfaRequiredScore;

    @Column(name = "token_revoke_score", nullable = false)
    private int tokenRevokeScore;

    @Column(name = "login_block_score", nullable = false)
    private int loginBlockScore;

    @Column(name = "revoke_all_sessions_score", nullable = false)
    private int revokeAllSessionsScore;

    @Column(name = "auto_response_enabled", nullable = false)
    private boolean autoResponseEnabled;

    @Column(name = "first_login_new_ip_exempt", nullable = false)
    private boolean firstLoginNewIpExempt;

    @Column(name = "first_login_new_user_agent_exempt", nullable = false)
    private boolean firstLoginNewUserAgentExempt;

    @Column(name = "token_reuse_force_critical", nullable = false)
    private boolean tokenReuseForceCritical;

    @Column(name = "token_reuse_revoke_all_sessions", nullable = false)
    private boolean tokenReuseRevokeAllSessions;

    @Column(name = "token_context_change_revoke_family", nullable = false)
    private boolean tokenContextChangeRevokeFamily;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static RiskDetectionSettingsEntity defaults() {
        return RiskDetectionSettingsEntity.builder()
                .id(SETTINGS_ID)
                .mediumThreshold(30)
                .highThreshold(60)
                .criticalThreshold(80)
                .mfaRequiredScore(60)
                .tokenRevokeScore(80)
                .loginBlockScore(80)
                .revokeAllSessionsScore(90)
                .autoResponseEnabled(true)
                .firstLoginNewIpExempt(true)
                .firstLoginNewUserAgentExempt(true)
                .tokenReuseForceCritical(true)
                .tokenReuseRevokeAllSessions(true)
                .tokenContextChangeRevokeFamily(true)
                .build();
    }
}
