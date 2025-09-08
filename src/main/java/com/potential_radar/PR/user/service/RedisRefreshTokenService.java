package com.potential_radar.PR.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 🔄 Redis 기반 Refresh Token 관리 서비스
 * 
 * 기존 데이터베이스 대신 Redis를 사용하여 Refresh Token을 관리합니다.
 * Redis의 TTL(Time To Live) 기능을 활용해 만료된 토큰이 자동으로 삭제됩니다.
 * 
 * Redis 저장 구조:
 * - Key: "refresh_token:{userId}" (예: "refresh_token:123")
 * - Value: RefreshTokenInfo JSON (토큰 값, 생성 시간, 사용 횟수 등)
 * - TTL: 14일 자동 만료
 * 
 * 보안 기능:
 * - 토큰 회전(Rotation): 토큰 사용 시마다 새 토큰 발급
 * - 재사용 탐지: 이미 사용된 토큰 재사용 시 모든 토큰 무효화
 * - 사용자별 단일 토큰: 한 사용자당 하나의 활성 토큰만 유지
 * - 자동 만료: Redis TTL로 만료된 토큰 자동 정리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 🔑 Redis 키 접두사 - 사용자별 토큰을 구분하는 네임스페이스
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    // 🔑 토큰 매핑 키 접두사 - 토큰으로 사용자 ID를 조회하는 역방향 매핑
    private static final String TOKEN_MAPPING_PREFIX = "token_mapping:";
    
    // 🔑 사용된 토큰 추적 키 접두사 - 재사용 탐지를 위한 블랙리스트
    private static final String USED_TOKEN_PREFIX = "used_token:";
    
    // ⏰ Refresh Token 만료 시간 (14일)
    private static final long REFRESH_TOKEN_TTL = 14L;
    
    /**
     * 🏭 새로운 Refresh Token 생성 및 저장
     * 
     * 사용자를 위한 새로운 Refresh Token을 생성하고 Redis에 저장합니다.
     * 기존 토큰이 있다면 덮어쓰며, 토큰 매핑도 함께 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @return 생성된 UUID 기반 Refresh Token 문자열
     */
    public String createRefreshToken(Long userId) {
        // 🎫 UUID 기반 고유 토큰 생성
        String refreshToken = UUID.randomUUID().toString();
        
        // 🗑️ 기존 토큰이 있다면 매핑 정보 삭제
        deleteExistingTokenMapping(userId);
        
        // 📦 토큰 정보 객체 생성 - 메타데이터 포함
        RefreshTokenInfo tokenInfo = RefreshTokenInfo.builder()
            .userId(userId)
            .token(refreshToken)
            .createdAt(System.currentTimeMillis())
            .usedCount(0)                      // 🔢 사용 횟수 추적 (재사용 탐지용)
            .build();
        
        // 💾 사용자 ID → 토큰 정보 저장 (메인 저장소)
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(userKey, tokenInfo, REFRESH_TOKEN_TTL, TimeUnit.DAYS);
        
        // 🔄 토큰 → 사용자 ID 역방향 매핑 저장 (빠른 조회용)
        String tokenKey = TOKEN_MAPPING_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(tokenKey, userId, REFRESH_TOKEN_TTL, TimeUnit.DAYS);
        
        log.info("🎫 사용자 {}의 새 Refresh Token 생성됨: {}", userId, refreshToken.substring(0, 8) + "...");
        return refreshToken;
    }

    /**
     * 🔍 Refresh Token으로 사용자 ID 조회
     * 
     * 토큰 문자열을 받아서 해당 토큰을 소유한 사용자 ID를 반환합니다.
     * 토큰 유효성 검증과 재사용 탐지도 함께 수행됩니다.
     * 
     * @param refreshToken 조회할 Refresh Token 문자열
     * @return 토큰 소유자의 사용자 ID, 유효하지 않으면 null
     */
    public Long getUserIdByToken(String refreshToken) {
        // 🔍 토큰이 이미 사용되었는지 확인 (재사용 탐지)
        if (isTokenUsed(refreshToken)) {
            log.warn("⚠️ 이미 사용된 토큰 재사용 시도: {}", refreshToken.substring(0, 8) + "...");
            // 🚨 재사용 탐지 시 해당 사용자의 모든 토큰 무효화
            Long userId = getUserIdFromMapping(refreshToken);
            if (userId != null) {
                revokeAllTokensForUser(userId);
            }
            return null;
        }
        
        // 🔄 토큰 매핑에서 사용자 ID 조회
        return getUserIdFromMapping(refreshToken);
    }

    /**
     * 🔄 Refresh Token 회전 (사용 후 새 토큰 발급)
     * 
     * 기존 토큰을 무효화하고 새로운 토큰을 발급합니다.
     * 토큰 탈취 위험을 줄이기 위해 매 사용마다 새 토큰을 생성합니다.
     * 
     * @param oldToken 사용된 기존 토큰
     * @return 새로 발급된 토큰, 실패 시 null
     */
    public String rotateRefreshToken(String oldToken) {
        Long userId = getUserIdFromMapping(oldToken);
        if (userId == null) {
            log.warn("❌ 유효하지 않은 토큰으로 회전 시도: {}", oldToken.substring(0, 8) + "...");
            return null;
        }
        
        // 🏷️ 기존 토큰을 사용됨으로 표시 (재사용 탐지용)
        markTokenAsUsed(oldToken);
        
        // 🎫 새로운 토큰 생성 및 반환
        String newToken = createRefreshToken(userId);
        log.info("🔄 사용자 {}의 토큰 회전 완료: {} → {}", 
            userId, oldToken.substring(0, 8) + "...", newToken.substring(0, 8) + "...");
        
        return newToken;
    }

    /**
     * 🗑️ 특정 사용자의 모든 Refresh Token 삭제
     * 
     * 로그아웃이나 보안 위반 시 사용자의 모든 토큰을 무효화합니다.
     * 
     * @param userId 토큰을 삭제할 사용자 ID
     */
    public void revokeAllTokensForUser(Long userId) {
        // 🔍 기존 토큰 정보 조회
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        RefreshTokenInfo tokenInfo = (RefreshTokenInfo) redisTemplate.opsForValue().get(userKey);
        
        if (tokenInfo != null) {
            // 🗑️ 토큰 매핑 삭제
            String tokenKey = TOKEN_MAPPING_PREFIX + tokenInfo.getToken();
            redisTemplate.delete(tokenKey);
            
            // 🏷️ 토큰을 사용됨으로 표시
            markTokenAsUsed(tokenInfo.getToken());
        }
        
        // 🗑️ 사용자 토큰 정보 삭제
        redisTemplate.delete(userKey);
        
        log.info("🗑️ 사용자 {}의 모든 Refresh Token 삭제됨", userId);
    }

    /**
     * ✅ Refresh Token 유효성 검증
     * 
     * 토큰이 존재하고 만료되지 않았는지 확인합니다.
     * 
     * @param refreshToken 검증할 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidToken(String refreshToken) {
        // 🚫 사용된 토큰인지 확인
        if (isTokenUsed(refreshToken)) {
            return false;
        }
        
        // 🔍 토큰 매핑 존재 여부 확인
        Long userId = getUserIdFromMapping(refreshToken);
        if (userId == null) {
            return false;
        }
        
        // 🔍 사용자 토큰 정보 존재 여부 확인
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        RefreshTokenInfo tokenInfo = (RefreshTokenInfo) redisTemplate.opsForValue().get(userKey);
        
        return tokenInfo != null && tokenInfo.getToken().equals(refreshToken);
    }

    // === 🔧 내부 헬퍼 메소드들 ===

    /**
     * 🔍 기존 토큰 매핑 정보 삭제 (내부용)
     */
    private void deleteExistingTokenMapping(Long userId) {
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        RefreshTokenInfo existingToken = (RefreshTokenInfo) redisTemplate.opsForValue().get(userKey);
        
        if (existingToken != null) {
            String oldTokenKey = TOKEN_MAPPING_PREFIX + existingToken.getToken();
            redisTemplate.delete(oldTokenKey);
        }
    }

    /**
     * 🔄 토큰 매핑에서 사용자 ID 조회 (내부용)
     */
    private Long getUserIdFromMapping(String refreshToken) {
        String tokenKey = TOKEN_MAPPING_PREFIX + refreshToken;
        Object userId = redisTemplate.opsForValue().get(tokenKey);
        return userId != null ? (Long) userId : null;
    }

    /**
     * 🏷️ 토큰을 사용됨으로 표시 (재사용 탐지용)
     */
    private void markTokenAsUsed(String refreshToken) {
        String usedTokenKey = USED_TOKEN_PREFIX + refreshToken;
        // 🗑️ 사용된 토큰 정보를 7일간 보관 (재사용 탐지용)
        redisTemplate.opsForValue().set(usedTokenKey, true, 7L, TimeUnit.DAYS);
    }

    /**
     * 🔍 토큰이 이미 사용되었는지 확인 (재사용 탐지용)
     */
    private boolean isTokenUsed(String refreshToken) {
        String usedTokenKey = USED_TOKEN_PREFIX + refreshToken;
        Boolean isUsed = (Boolean) redisTemplate.opsForValue().get(usedTokenKey);
        return Boolean.TRUE.equals(isUsed);
    }

    /**
     * 📦 Refresh Token 메타데이터를 담는 내부 클래스
     * 
     * Redis에 저장될 토큰 정보를 구조화합니다.
     * 보안 추적과 디버깅에 필요한 메타데이터를 포함합니다.
     */
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RefreshTokenInfo {
        private Long userId;           // 👤 토큰 소유자 ID
        private String token;          // 🎫 실제 토큰 값
        private Long createdAt;        // 📅 생성 시간 (타임스탬프)
        private Integer usedCount;     // 🔢 사용 횟수 (재사용 탐지용)
    }
}