package com.example.authapp.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authapp.domain.jwt.dto.JWTResponseDTO;
import com.example.authapp.application.auth.usecase.JwtUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jwt")
public class JwtController {

    private final JwtUseCase jwtService;

    // 소셜 로그인 refresh cookie를 access token 응답으로 교환한다.
    @PostMapping("/exchange")
    public JWTResponseDTO jwtExchangeApi(HttpServletRequest request,HttpServletResponse response){
        return jwtService.cookie2Header(request, response);
    }

    // refresh token을 회전하고 access token을 재발급한다.
    @PostMapping("/refresh")
    public JWTResponseDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        return jwtService.refreshRotate(request, response);
    }

}
