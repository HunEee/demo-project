package com.example.authapp.domain.user.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.example.authapp.domain.user.dto.UserRequestDTO;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_entity")
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
    private Boolean locked;
    @Column(name = "enabled",nullable = false)
    private Boolean enabled;

    
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();
    
    
    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    //********************************************************************************
    // 커스텀 메서드
    //********************************************************************************
    
    // 수정 메소드
    public void updateUser(UserRequestDTO dto) {
        if (dto.getEmail() != null) {this.email = dto.getEmail();}
        if (dto.getNickname() != null) {this.nickname = dto.getNickname();}
    }

    // Role 추가(양방향 세팅)
    public void addRole(RoleEntity role) {
        if (role == null) return;

        if (!this.roles.contains(role)) {
            this.roles.add(role);
            role.getUsers().add(this);
        }
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
    
}
