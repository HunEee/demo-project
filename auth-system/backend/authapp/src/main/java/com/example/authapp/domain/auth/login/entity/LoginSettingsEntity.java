package com.example.authapp.domain.auth.login.entity;

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
@Table(name = "login_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "local_password_login_enabled", nullable = false)
    private boolean localPasswordLoginEnabled;

    @Column(name = "social_login_enabled", nullable = false)
    private boolean socialLoginEnabled;

    @Column(name = "signup_enabled", nullable = false)
    private boolean signupEnabled;

    @Column(name = "remember_me_enabled", nullable = false)
    private boolean rememberMeEnabled;

    @Column(name = "default_post_login_route", nullable = false)
    private String defaultPostLoginRoute;

    @Column(name = "generic_login_failure_message_enforced", nullable = false)
    private boolean genericLoginFailureMessageEnforced;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static LoginSettingsEntity defaults() {
        return LoginSettingsEntity.builder()
                .id(SETTINGS_ID)
                .localPasswordLoginEnabled(true)
                .socialLoginEnabled(true)
                .signupEnabled(true)
                .rememberMeEnabled(false)
                .defaultPostLoginRoute("/dashboard")
                .genericLoginFailureMessageEnforced(true)
                .build();
    }
}
