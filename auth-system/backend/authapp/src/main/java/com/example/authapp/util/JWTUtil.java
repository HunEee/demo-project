package com.example.authapp.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    
    // 토큰 만료시간 조회 메서드(초 단위로 변환)
    public static long getAccessTokenExpiresIn() {
        return accessTokenExpiresIn / 1000; 
    }

    public static long getRefreshTokenExpiresIn() {
        return refreshTokenExpiresIn / 1000;
    }
    

    // JWT 클레임 username 파싱
    public static String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    // JWT 클레임 roles 파싱
    @SuppressWarnings("unchecked") // 타입 안정성 경고 무시
    public static Set<String> getRoles (String token) {
    	Claims claims =  Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return new HashSet<>( claims.get("roles",List.class));
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

    // jti 주입 버전 추가
    public static String createJWT(String username, Set<String> roles , String jti, Boolean isAccess) {
        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
        String type = isAccess ? "access" : "refresh";
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("type", type)
                .id(jti) // 외부에서 받은 jti 사용
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }

}
