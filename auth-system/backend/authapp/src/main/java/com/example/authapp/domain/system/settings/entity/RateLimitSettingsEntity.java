package com.example.authapp.domain.system.settings.entity;

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
@Table(name = "rate_limit_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "login_per_ip_per_minute", nullable = false)
    private int loginPerIpPerMinute;

    @Column(name = "login_per_username_per_minute", nullable = false)
    private int loginPerUsernamePerMinute;

    @Column(name = "refresh_per_session_per_minute", nullable = false)
    private int refreshPerSessionPerMinute;

    @Column(name = "password_reset_per_email_per_day", nullable = false)
    private int passwordResetPerEmailPerDay;

    @Column(name = "verification_email_per_email_per_day", nullable = false)
    private int verificationEmailPerEmailPerDay;

    @Column(name = "admin_write_per_minute", nullable = false)
    private int adminWritePerMinute;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static RateLimitSettingsEntity defaults() {
        return RateLimitSettingsEntity.builder()
                .id(SETTINGS_ID)
                .loginPerIpPerMinute(20)
                .loginPerUsernamePerMinute(10)
                .refreshPerSessionPerMinute(30)
                .passwordResetPerEmailPerDay(5)
                .verificationEmailPerEmailPerDay(10)
                .adminWritePerMinute(60)
                .build();
    }
}
