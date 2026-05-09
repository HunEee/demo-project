package com.example.authapp.domain.user.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", unique = true, nullable = false, updatable = false)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "email")
    private String email;
    @Column(name = "nickname")
    private String nickname;
    @Column(name = "profile_image", length = 1000)
    private String profileImage;
    
    
    @Column(name = "locked", nullable = false)
    private Boolean locked;		// 계정 잠금 처리
    @Column(name = "enabled",nullable = false)
    private Boolean enabled;	// 계정 탈퇴시 false 처리

    
    @Column(name = "is_social", nullable = false)
    private Boolean isSocial;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider_type", nullable = true)
    private SocialProviderType socialProviderType;
    
    @Column(name = "provider_id", nullable = true)
    private String providerId; // 소셜 고유 ID

    
	/*
	 * @Enumerated(EnumType.STRING)
	 * 
	 * @Column(name = "role_type", nullable = false) private UserRoleType roleType;
	 */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();
    
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    //********************************************************************************
    // 커스텀 메서드
    //********************************************************************************
    
    // 프로필 수정 메소드
    public void updateProfile(String nickname, String profileImage) {
        this.nickname = nickname;
        this.profileImage = profileImage;
    }
    
    // Role 추가(양방향 세팅)
    public void addRole(RoleEntity role) {
        if (role == null) return;

        if (!this.roles.contains(role)) {
            this.roles.add(role);
            role.getUsers().add(this);
        }
    }
    
    // 패스워드 변경
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
    
    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public void updateOAuthProfile(String email,String nickname) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }
    
    
    // 탈퇴 여부 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
    
    // 탈퇴 메서드
    public void deactivate() {
        this.enabled = false;
        this.locked = true;
        this.deletedAt = LocalDateTime.now();
    }
    
    
}
