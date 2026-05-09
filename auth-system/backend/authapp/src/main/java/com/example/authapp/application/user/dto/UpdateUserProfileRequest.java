package com.example.authapp.application.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
	    @NotBlank String nickname,
	    String profileImage
) {}