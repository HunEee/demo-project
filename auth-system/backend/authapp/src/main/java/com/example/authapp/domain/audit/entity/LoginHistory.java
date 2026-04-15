package com.example.authapp.domain.audit.entity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "login_history",
    indexes = {
        @Index(name = "idx_login_user_id", columnList = "username"),
        @Index(name = "idx_login_created_at", columnList = "login_at"),
        @Index(name = "idx_login_status", columnList = "status")
    }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User 엔티티와 직접 연관관계를 맺지 않음 -> 결합도 낮춤
    @Column(name = "username", nullable = false)
    private String username;

    // 로그인 시각
    @CreatedDate
    @Column(name = "login_at", nullable = false, updatable = false)
    private LocalDateTime loginAt;

    // 로그아웃 시각
    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    // 로그인 성공 여부
    @Column(name = "success", nullable = false)
    private boolean success;

    // 실패 사유 (비번 틀림 등)
    @Column(name = "fail_reason")
    private String failReason;

    // IP
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // 브라우저 정보
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    // 디바이스 (Mobile / Windows 등)
    @Column(name = "device")
    private String device;

    // 국가 (확장용)
    @Column(name = "location")
    private String location;

    //로그인 상태 (ACTIVE / LOGOUT / EXPIRED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoginStatus status;

    //********************************************************************************
    // 상태 변경 메서드
    //********************************************************************************

    public void logout() {
        this.logoutAt = LocalDateTime.now();
        this.status = LoginStatus.LOGOUT;
    }

    public void expire() {
        this.status = LoginStatus.EXPIRED;
    }

    public void fail(String reason) {
        this.success = false;
        this.failReason = reason;
        this.status = LoginStatus.FAILED;
    }
}