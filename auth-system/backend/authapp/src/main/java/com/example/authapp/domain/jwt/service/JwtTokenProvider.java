package com.example.authapp.domain.jwt.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JwtTokenProvider {

    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TYPE = "type";

    private final SecretKey secretKey;
    private final TokenSettingsService tokenSettingsService;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:${JWT_SECRET:himynameiskimjihunsecuritykeyann}}") String secret,
            TokenSettingsService tokenSettingsService
    ) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        this.tokenSettingsService = tokenSettingsService;
    }

    public long accessTokenExpiresInSeconds() {
        return Duration.ofMinutes(tokenSettingsService.current().getAccessTokenLifetimeMinutes()).toSeconds();
    }

    public long refreshTokenExpiresInSeconds() {
        return Duration.ofDays(tokenSettingsService.current().getRefreshTokenLifetimeDays()).toSeconds();
    }

    public Claims validate(String token, boolean access) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get(CLAIM_TYPE, String.class);
        if (type == null) {
            throw new JwtException("Token type is missing");
        }
        if (access && !ACCESS.equals(type)) {
            throw new JwtException("Invalid access token type");
        }
        if (!access && !REFRESH.equals(type)) {
            throw new JwtException("Invalid refresh token type");
        }
        return claims;
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRoles(String token) {
        return new HashSet<>(parse(token).get(CLAIM_ROLES, List.class));
    }

    public String getJti(String token) {
        return parse(token).getId();
    }

    public String createToken(String username, Set<String> roles, String jti, boolean access) {
        long now = System.currentTimeMillis();
        long expiryMillis = access
                ? Duration.ofMinutes(tokenSettingsService.current().getAccessTokenLifetimeMinutes()).toMillis()
                : Duration.ofDays(tokenSettingsService.current().getRefreshTokenLifetimeDays()).toMillis();
        String type = access ? ACCESS : REFRESH;

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type)
                .id(jti)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMillis))
                .signWith(secretKey)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
