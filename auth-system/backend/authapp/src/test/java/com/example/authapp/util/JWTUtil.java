package com.example.authapp.util;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public final class JWTUtil {

    private static final SecretKey SECRET_KEY = new SecretKeySpec(
            "himynameiskimjihunsecuritykeyann".getBytes(StandardCharsets.UTF_8),
            Jwts.SIG.HS256.key().build().getAlgorithm()
    );

    private JWTUtil() {
    }

    public static String createJWT(String username, Set<String> roles, String jti, Boolean isAccess) {
        long now = System.currentTimeMillis();
        long expiry = isAccess ? Duration.ofHours(1).toMillis() : Duration.ofDays(7).toMillis();
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("type", isAccess ? "access" : "refresh")
                .id(jti)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String getJti(String token) {
        return parse(token).getId();
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getRoles(String token) {
        return new HashSet<>(parse(token).get("roles", List.class));
    }

    private static Claims parse(String token) {
        return Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();
    }
}
