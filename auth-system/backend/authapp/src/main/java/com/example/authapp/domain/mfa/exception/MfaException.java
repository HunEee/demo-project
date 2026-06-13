package com.example.authapp.domain.mfa.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class MfaException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public MfaException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static MfaException badRequest(String code, String message) {
        return new MfaException(HttpStatus.BAD_REQUEST, code, message);
    }
}
