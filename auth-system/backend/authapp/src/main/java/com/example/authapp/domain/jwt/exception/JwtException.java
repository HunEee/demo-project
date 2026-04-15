package com.example.authapp.domain.jwt.exception;

import com.example.authapp.global.exception.CustomException;

public class JwtException extends CustomException {

    public JwtException(int status, String message) {
        super(status, message);
    }

    //**************************************************************************
    // Access Token 관련
    //**************************************************************************
    public static JwtException invalidAccessToken() {
        return new JwtException(401, "유효하지 않은 access token입니다.");
    }

    public static JwtException expiredAccessToken() {
        return new JwtException(401, "access token이 만료되었습니다.");
    }

    //**************************************************************************
    // Refresh Token 관련
    //**************************************************************************
    public static JwtException expiredRefreshToken() {
        return new JwtException(401, "refresh token이 만료되었습니다.");
    }

    public static JwtException revokedRefreshToken() {
        return new JwtException(401, "이미 폐기된 refresh token입니다.");
    }
    
    public static JwtException invalidRefreshToken() {
        return new JwtException(401, "유효하지 않은 refresh token입니다.");
    }
    
    //**************************************************************************
    // 토큰 재발급 관련
    //**************************************************************************
    public static JwtException refreshCookieNotFound() {
        return new JwtException(400, "refresh cookie가 존재하지 않습니다.");
    }
    
    public static JwtException refreshTokenNotFound() {
        return new JwtException(404, "refresh token이 존재하지 않습니다.");
    }
    
    public static JwtException tokenNotFound() {
        return new JwtException(404, "해당 토큰을 찾을 수 없습니다.");
    }

    public static JwtException tokenMismatch() {
        return new JwtException(401, "토큰 정보가 일치하지 않습니다.");
    }
    
    //**************************************************************************
    // 보안 관련 
    //**************************************************************************
    public static JwtException abnormalAccess() {
        return new JwtException(403, "보안 경고: 비정상 접근이 감지되었습니다.");
    }
    
}