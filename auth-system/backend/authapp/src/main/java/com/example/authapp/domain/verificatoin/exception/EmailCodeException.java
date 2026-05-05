package com.example.authapp.domain.verificatoin.exception;

import com.example.authapp.global.exception.CustomException;

public class EmailCodeException extends CustomException {

    public EmailCodeException(int status, String message) {
        super(status, message);
    }

    public static EmailCodeException codeNotFound() {
        return new EmailCodeException(404, "인증코드를 찾을 수 없습니다.");
    }

    public static EmailCodeException codeExpired() {
        return new EmailCodeException(400, "인증코드가 만료되었습니다.");
    }

    public static EmailCodeException invalidCode() {
        return new EmailCodeException(400, "인증코드가 올바르지 않습니다.");
    }

    public static EmailCodeException alreadyUsed() {
        return new EmailCodeException(400, "이미 사용된 인증코드입니다.");
    }

    public static EmailCodeException tooManyRequests() {
        return new EmailCodeException(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
    
}
