package com.example.authapp.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.authapp.security.principal.CustomUserDetails;
import com.example.authapp.security.principal.CustomUserDetailsService;
import com.example.authapp.security.principal.UserPrincipal;
import com.example.authapp.util.JWTUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private final CustomUserDetailsService userDetailsService;
	
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
        	sendError(response, "INVALID_TOKEN", "Invalid Authorization header");
            throw new ServletException("Invalid JWT token");
        }

        // 토큰 파싱
        String accessToken = authorization.split(" ")[1];

        try {
            // 여기서 검증 (예외 발생 기준) -> true면 access 토큰 검증
            JWTUtil.validate(accessToken,true); 

            String username = JWTUtil.getUsername(accessToken);
            String jti = JWTUtil.getJti(accessToken);

            UserPrincipal userPrincipal = userDetailsService.loadUserByUsername(username);
            userPrincipal.setJti(jti);
            Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // 토큰 만료
            sendError(response, "TOKEN_EXPIRED", "Access token expired");
        } catch (JwtException | IllegalArgumentException e) {
            // 위조, 형식 오류 등
            sendError(response, "INVALID_TOKEN", "Invalid token");
        } catch (Exception e) {
            // 기타 서버 에러
            sendError(response, "AUTH_ERROR", "Authentication error");
        }
        
    }
    
    
    // 공통 에러 응답 메서드
    private void sendError(HttpServletResponse response,String code,String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            String.format("{\"code\":\"%s\",\"message\":\"%s\"}", code, message)
        );
    }
    

}