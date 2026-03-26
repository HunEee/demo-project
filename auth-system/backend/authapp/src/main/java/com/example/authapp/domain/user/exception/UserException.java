package com.example.authapp.domain.user.exception;

import com.example.authapp.global.exception.CustomException;

public class UserException extends CustomException {

    public UserException(int status, String message) {
        super(status, message);
    }

    public static UserException duplicateUsername() {
        return new UserException(400, "이미 유저가 존재합니다.");
    }

    public static UserException duplicateEmail() {
        return new UserException(400, "이미 이메일이 존재합니다.");
    }

    public static UserException userNotFound() {
        return new UserException(404, "사용자를 찾을 수 없습니다.");
    }
    
    public static UserException roleNotFound() {
        return new UserException(500, "기본 권한이 존재하지 않습니다.");
    }
    
    
}
