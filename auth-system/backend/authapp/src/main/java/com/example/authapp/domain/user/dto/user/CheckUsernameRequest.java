package com.example.authapp.domain.user.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckUsernameRequest(
	    @NotBlank @Size(min = 4) String username
) {}
