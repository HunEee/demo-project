package com.example.authapp.domain.jwt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.authapp.domain.audit.entity.LoginHistoryEntity;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "refresh_token",
        indexes = {
                // refresh token 조회 성능 개선
                @Index(name = "idx_refresh_token", columnList = "refresh", unique = true),

                // 사용자별 refresh token 조회
                @Index(name = "idx_refresh_username", columnList = "username"),

                // 만료 토큰 정리 작업에서 사용
                @Index(name = "idx_refresh_expires_at", columnList = "expires_at")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User 엔티티와 직접 연관관계를 맺지 않음 -> 결합도 낮춤
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "refresh", nullable = false, length = 512)
    private String refresh;

    // jwt_id -> 토큰 중복 방지 및 추적을 위해 사용
    @Column(nullable = false, unique = true)
    private String jti;
    
    //토큰 생성 시간
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // 토큰 만료 시간 
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 토큰 폐기 여부 -> 로그아웃 또는 토큰 rotation 시 true
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
    
    //Refresh Token Rotation 시 새로 발급된 토큰 값을 저장
    @Column(name = "replaced_by_token")
    private String replacedByToken;
    
    // 로그인 디바이스 정보
    @Column(name = "device")
    private String device;
    
    // 로그인한 클라이언트 ID
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    //브라우저 / 클라이언트 정보
    @Column(name = "user_agent", length = 512)
    private String userAgent;
    
    // 마지막 사용 시간 (세션 관리용)
    @LastModifiedDate
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    //********************************************************************************
    // 연관관계 칼럼
    //********************************************************************************
    
    // 로그인 이력 연결 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_history_id")
    private LoginHistoryEntity loginHistory;
    
    //********************************************************************************
    // 커스텀 메서드
    //********************************************************************************
    
    // 토큰 만료시키기
    public void revoke() {
        this.revoked = true;
    }
    
    // Refresh Token Rotation 시 새 토큰 연결
    public void setReplacedByToken(String replacedByToken) {
        this.replacedByToken = replacedByToken;
    }
    
    // 사용 시간 업데이트
    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }

}