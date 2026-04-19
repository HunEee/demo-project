package com.example.authapp.domain.audit.dto;

import java.time.LocalDateTime;

import com.example.authapp.domain.audit.entity.LoginStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 페이지 응답용
@Getter
@AllArgsConstructor
public class LoginHistoryResponseDTO {

    private Long id;
    private String username;
    private String ip;
    private String userAgent;
    private String device;
    private String location;
    private LocalDateTime loginAt;
    private LoginStatus status;

}
