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
@Table(name = "web_security_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSecuritySettingsEntity {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "settings_id")
    private Long id;

    @Column(name = "allow_credentials", nullable = false)
    private boolean allowCredentials;

    @Column(name = "allowed_methods", nullable = false)
    private String allowedMethods;

    @Column(name = "allowed_origins", length = 2000)
    private String allowedOrigins;

    @Column(name = "allowed_redirect_uris", length = 4000)
    private String allowedRedirectUris;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static WebSecuritySettingsEntity defaults() {
        return WebSecuritySettingsEntity.builder()
                .id(SETTINGS_ID)
                .allowCredentials(true)
                .allowedMethods("GET,POST,PUT,PATCH,DELETE,OPTIONS")
                .allowedOrigins("http://localhost:5173")
                .allowedRedirectUris("")
                .build();
    }
}
