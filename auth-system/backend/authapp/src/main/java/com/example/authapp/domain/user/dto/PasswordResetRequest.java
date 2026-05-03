package com.example.authapp.domain.user.dto;

import lombok.Getter;

@Getter
public class PasswordResetRequest {
	private String username;
	private String email;
	private String verifyCode;
    private String newPassword;
    
}
