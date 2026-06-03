package com.example.authapp.domain.user.dto.password;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
	    @NotBlank String currentPassword,
	    @NotBlank String newPassword
) {}
