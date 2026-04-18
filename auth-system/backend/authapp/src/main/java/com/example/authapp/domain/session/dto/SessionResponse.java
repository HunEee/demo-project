package com.example.authapp.domain.session.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResponse {

    private Long id;
    private String ip;
    private String device;
    private LocalDateTime createdAt;
    private String lastAccessAt;
    private boolean current;
    
}
