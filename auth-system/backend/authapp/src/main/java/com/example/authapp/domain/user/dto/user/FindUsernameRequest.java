package com.example.authapp.domain.user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FindUsernameRequest(
	    @NotBlank @Email String email,
	    @NotBlank String verificationCode
) {}
