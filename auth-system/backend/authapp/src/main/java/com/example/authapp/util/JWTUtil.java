package com.example.authapp.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class JWTUtil {

	// 토큰 타입 상수
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
	
    private static final SecretKey secretKey;
    private static final Long accessTokenExpiresIn;
    private static final Long refreshTokenExpiresIn;

    static  {
        String secretKeyString = "himynameiskimjihunsecuritykeyann";
        secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        accessTokenExpiresIn = 3600L * 10; // 1시간
        refreshTokenExpiresIn = 604800L * 1000; // 7일
    }

    // JWT 클레임 username 파싱
    public static String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    // JWT 클레임 role 파싱
    public static String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }
    
    // JWT_ID 파싱
    public static String getJti(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getId();
    }

    // JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
    public static Claims validate(String token, Boolean isAccess) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get("type", String.class);

        if (type == null) {
            throw new JwtException("Token type 누락");
        }
        if (isAccess && !ACCESS.equals(type)) {
            throw new JwtException("유효하지 않은 access token");
        }
        if (!isAccess && !REFRESH.equals(type)) {
            throw new JwtException("유효하지 않은 refresh token");
        }
        return claims;
    }

    // JWT(Access/Refresh) 생성
    public static String createJWT(String username, String role, Boolean isAccess) {

        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
        		.subject(username)
                .claim("role", role)
                .claim("type", type)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }
    

}
