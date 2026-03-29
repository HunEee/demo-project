package com.example.authapp.handler.dto;

import com.example.authapp.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🔥 Entity → DTO 변환
    public static UserResponseDTO from(UserEntity user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .image(user.getProfileImage())
                .enabled(user.getEnabled())
                .provider(user.getIsSocial() ? user.getSocialProviderType().name() : "LOCAL")
                .createdAt(user.getCreatedDate())
                .updatedAt(user.getUpdatedDate())
                .build();
    }
    
}