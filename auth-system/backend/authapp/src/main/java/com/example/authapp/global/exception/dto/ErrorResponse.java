package com.example.authapp.global.exception.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
	private int status;
    private String message;
    private String code;

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}
