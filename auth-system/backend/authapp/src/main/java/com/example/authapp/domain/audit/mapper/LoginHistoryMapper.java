package com.example.authapp.domain.audit.mapper;

import com.example.authapp.domain.audit.dto.LoginHistoryResponseDTO;
import com.example.authapp.domain.audit.entity.LoginHistoryEntity;

public class LoginHistoryMapper {
    
    // 로그인 기록 페이지 응답용
    public static LoginHistoryResponseDTO toDTO(LoginHistoryEntity entity) {
        return new LoginHistoryResponseDTO(
            entity.getId(),
            entity.getUsername(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getDevice(),
            entity.getLocation(),
            entity.getLoginAt(),
            entity.getStatus()
        );
    }
	
}
