package com.example.authapp.security.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.authapp.domain.jwt.service.RefreshTokenService;
import com.example.authapp.domain.jwt.service.JwtTokenProvider;
import com.example.authapp.security.principal.UserPrincipal;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JWTFilter extends OncePerRequestFilter {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    public JWTFilter(RefreshTokenService refreshTokenService, JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            sendError(response, "INVALID_TOKEN", "Invalid Authorization header");
            return;
        }

        String accessToken = authorization.split(" ")[1];

        try {
            jwtTokenProvider.validate(accessToken, true);

            String username = jwtTokenProvider.getUsername(accessToken);
            String jti = jwtTokenProvider.getJti(accessToken);
            if (!refreshTokenService.existsActiveSession(username, jti)) {
                sendError(response, "SESSION_REVOKED", "Session revoked");
                return;
            }

            var authorities = jwtTokenProvider.getRoles(accessToken)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UserPrincipal userPrincipal = new UserPrincipal(username, authorities);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    userPrincipal.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            sendError(response, "TOKEN_EXPIRED", "Access token expired");
        } catch (JwtException | IllegalArgumentException e) {
            sendError(response, "INVALID_TOKEN", "Invalid token");
        } catch (Exception e) {
            sendError(response, "AUTH_ERROR", "Authentication error");
        }
    }

    private void sendError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":\"%s\",\"message\":\"%s\"}", code, message));
    }
}
