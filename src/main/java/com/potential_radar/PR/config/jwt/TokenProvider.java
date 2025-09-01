package com.potential_radar.PR.config.jwt;

import com.potential_radar.PR.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * 🎫 JWT 토큰 제공자 서비스
 * 
 * JWT 토큰의 생성, 검증, 파싱을 담당하는 핵심 서비스입니다.
 * 
 * 주요 기능:
 * - Access Token 생성 (1시간 유효)
 * - JWT 토큰 서명 검증
 * - 토큰에서 사용자 정보 추출
 * - Spring Security 인증 객체 생성
 * 
 * 보안 고려사항:
 * - HMAC-SHA256 알고리즘 사용
 * - 256비트 이상의 비밀 키 사용
 * - 토큰 만료 시간 검증
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Getter
public class TokenProvider {
    // 📄 JWT 설정 정보를 담는 설정 클래스
    private final JwtProperties jwtProperties;
    // 🔑 JWT 서명에 사용될 비밀 키 (애플리케이션 시작 시 초기화)
    private Key secretKey;

    @PostConstruct
    public void init() {
        if (jwtProperties.getSecretKey() == null || jwtProperties.getSecretKey().isEmpty()) {
            throw new IllegalStateException("JWT secret key must not be null or empty");
        }
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user, Duration expiredAt) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user);
    }

    public String generateAccessToken(User user) {
        return generateToken(user, Duration.ofMillis(jwtProperties.getAccessTokenExpiration()));
    }

    /**
     * 🏭 JWT 토큰 실제 생성 내부 메소드
     * 
     * JWT 토큰의 헤더, 페이로드, 서명을 생성하여 완전한 토큰을 만듭니다.
     * 
     * 토큰 구성:
     * - Header: 토큰 타입(JWT), 알고리즘(HS256)
     * - Payload: 사용자 정보(email, userId), 만료시간, 발급자 등
     * - Signature: HMAC-SHA256 알고리즘으로 생성된 서명
     * 
     * @param expiredAt 토큰 만료 시점
     * @param user 토큰에 포함될 사용자 정보
     * @return 생성된 JWT 토큰 문자열
     */
    private String makeToken(Date expiredAt, User user) {
        Date now = new Date();

        return Jwts.builder()
                // 🏷️ 헤더 설정: 토큰 타입을 JWT로 지정
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                // 🏢 발급자 정보 설정
                .setIssuer(jwtProperties.getIssuer())
                // 📅 토큰 발급 시각
                .setIssuedAt(now)
                // ⏰ 토큰 만료 시각
                .setExpiration(expiredAt)
                // 👤 토큰 주체(사용자 이메일)
                .setSubject(user.getEmail())
                // 🏷️ 커스텀 클레임: 사용자 ID
                .claim("id", user.getUserId())
                // 🔐 HMAC-SHA256 알고리즘으로 서명
                .signWith(secretKey, SignatureAlgorithm.HS256)
                // 📎 최종 토큰 문자열로 압축
                .compact();
    }


    // JWT 토큰 유효성 검증 메서드
    public boolean validToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ Expired JWT token: {}", e.getMessage());
            return false;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("❌ Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("🔥 Unexpected error during token validation : {}", e.getMessage());
            return false;
        }
    }

    // 토큰 기반으로 인증 정보를 가져오는 메서드
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        // 토큰에서 권한 정보를 추출하거나 사용자별 권한 조회
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities), null, authorities);
    }

    // 토큰 기반으로 유저 ID를 가져오는 메서드
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);
    }


    private Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);

        }
    }
}
