package com.example.authapp.domain.user.exception;

import com.example.authapp.global.exception.CustomException;

public class UserException extends CustomException {

    public UserException(int status, String message) {
        super(status, message);
    }

    public static UserException usernameAlreadyExists() {
        return new UserException(400, "이미 사용 중인 아이디입니다.");
    }

    public static UserException emailAlreadyExists() {
        return new UserException(400, "이미 사용 중인 이메일입니다.");
    }

    public static UserException userNotFound() {
        return new UserException(404, "사용자를 찾을 수 없습니다.");
    }
    
    public static UserException roleNotFound() {
        return new UserException(500, "기본 권한이 존재하지 않습니다.");
    }

    public static UserException alreadyDeleted() {
        return new UserException(400, "이미 탈퇴한 계정입니다.");
    }

    public static UserException accountUnavailable() {
        return new UserException(403, "사용할 수 없는 계정입니다.");
    }

    public static UserException socialAccountConflict() {
        return new UserException(409, "소셜 계정 정보가 일치하지 않습니다.");
    }
    
}
