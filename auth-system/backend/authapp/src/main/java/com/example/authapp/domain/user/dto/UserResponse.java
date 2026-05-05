package com.example.authapp.domain.user.dto;

public record UserResponse(
		String username, 
		Boolean social, 
		String nickname, 
		String email
) {}