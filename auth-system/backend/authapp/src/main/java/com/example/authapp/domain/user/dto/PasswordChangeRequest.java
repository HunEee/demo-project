package com.example.authapp.domain.user.dto;

import lombok.Getter;

@Getter
public class PasswordChangeRequest {

    private String currentPassword;
    private String newPassword;
    
}
