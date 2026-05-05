package com.example.authapp.domain.user.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
	    @NotBlank String nickname,
	    String profileImage
) {}