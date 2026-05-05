package com.example.authapp.domain.user.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
	    @NotBlank String username,
	    @NotBlank @Email String email,
	    @NotBlank String verificationCode,
	    @NotBlank String newPassword
) {}
