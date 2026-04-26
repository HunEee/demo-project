package com.example.authapp.domain.mfa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;

@Entity
public class MfaEntity {

    private Long id;
    private String username;
    private Boolean enabled;
    private String secretKey; // TOTP용
    private String backupCodes;
    private LocalDateTime verifiedAt;
    
}
