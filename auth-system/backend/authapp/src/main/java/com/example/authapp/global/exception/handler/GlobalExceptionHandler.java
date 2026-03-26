package com.example.authapp.global.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.authapp.global.exception.CustomException;
import com.example.authapp.global.exception.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

// 통합예외처리
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1️. 권한 에러
	@ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
		log.warn(ex.toString()); 
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "접근 권한이 없습니다."));
    }

    // 2️. 커스텀 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleUserCustomException(CustomException ex) {
    	log.warn("UserException: {}", ex.getMessage());
    	return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getStatus(), ex.getMessage()));
    }

    // 3️. 잘못된 값
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    	log.warn(ex.toString()); 
    	return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, ex.getMessage()));
    }

    // 4️.모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    	log.error("서버 에러", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "서버 내부 오류가 발생했습니다."));
    }
    
}
    
    


