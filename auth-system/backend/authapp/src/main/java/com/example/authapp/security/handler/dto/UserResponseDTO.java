package com.example.authapp.security.handler.dto;

import com.example.authapp.domain.user.entity.RoleEntity;
import com.example.authapp.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String image;
    private Boolean enabled;
    private String provider;
    private Set<String> roles; 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity → DTO 변환
    public static UserResponseDTO from(UserEntity user) {
        return from(user, user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet()));
    }

    public static UserResponseDTO from(UserEntity user, Set<String> roles) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .image(user.getProfileImage())
                .enabled(user.isEnabled())
                .provider(user.isSocial() ? user.getSocialProviderType().name() : "LOCAL")
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
}
