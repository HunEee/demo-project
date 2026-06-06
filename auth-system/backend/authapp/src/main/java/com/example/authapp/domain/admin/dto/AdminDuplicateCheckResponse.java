package com.example.authapp.domain.admin.dto;

public record AdminDuplicateCheckResponse(
        String field,
        String value,
        boolean exists
) {
}
