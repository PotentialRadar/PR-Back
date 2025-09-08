package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.JwtProperties;
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
 * - TTL: application.yml 설정값 기반 자동 만료
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
    private final JwtProperties jwtProperties;
    
    // 🔑 Redis 키 접두사 - 사용자별 토큰을 구분하는 네임스페이스
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    // 🔑 토큰 매핑 키 접두사 - 토큰으로 사용자 ID를 조회하는 역방향 매핑
    private static final String TOKEN_MAPPING_PREFIX = "token_mapping:";
    
    // 🔑 사용된 토큰 추적 키 접두사 - 재사용 탐지를 위한 블랙리스트
    private static final String USED_TOKEN_PREFIX = "used_token:";
    
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
        
        // ⏰ 설정에서 TTL 정보 조회
        TTLInfo ttlInfo = getRefreshTokenTTL();
        
        // 💾 사용자 ID → 토큰 정보 저장 (메인 저장소)
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(userKey, tokenInfo, ttlInfo.value, ttlInfo.unit);
        
        // 🔄 토큰 → 사용자 ID 역방향 매핑 저장 (빠른 조회용)
        String tokenKey = TOKEN_MAPPING_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(tokenKey, userId, ttlInfo.value, ttlInfo.unit);
        
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
        Long usedTokenUserId = getUsedTokenUserId(refreshToken);
        if (usedTokenUserId != null) {
            log.warn("⚠️ 이미 사용된 토큰 재사용 시도: {} (원 소유자: {})", 
                refreshToken.substring(0, 8) + "...", usedTokenUserId);
            // 🚨 재사용 탐지 시 해당 사용자의 모든 토큰 무효화
            revokeAllTokensForUser(usedTokenUserId);
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
     * 
     * Redis에서 숫자가 Integer로 역직렬화될 수 있으므로 안전한 타입 변환을 수행합니다.
     */
    private Long getUserIdFromMapping(String refreshToken) {
        String tokenKey = TOKEN_MAPPING_PREFIX + refreshToken;
        Object userId = redisTemplate.opsForValue().get(tokenKey);
        
        if (userId == null) {
            return null;
        }
        
        // 🔧 안전한 타입 변환: Integer, Long, String 모두 지원
        try {
            if (userId instanceof Long) {
                return (Long) userId;
            } else if (userId instanceof Integer) {
                return ((Integer) userId).longValue(); // Integer → Long 안전 변환
            } else if (userId instanceof String) {
                return Long.parseLong((String) userId); // String → Long 변환
            } else if (userId instanceof Number) {
                return ((Number) userId).longValue(); // 기타 Number 타입 처리
            } else {
                log.warn("⚠️ 예상하지 못한 userId 타입: {} -> {}", userId.getClass().getSimpleName(), userId);
                return null;
            }
        } catch (NumberFormatException e) {
            log.error("🔥 userId 타입 변환 실패: {} -> {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 🏷️ 토큰을 사용됨으로 표시 (재사용 탐지용 - userId 포함)
     * 
     * 토큰 회전 시 기존 토큰을 "사용됨" 상태로 표시합니다.
     * 재사용 탐지 시 해당 사용자의 모든 토큰을 무효화할 수 있도록 userId도 함께 저장합니다.
     */
    private void markTokenAsUsed(String refreshToken) {
        // 먼저 현재 토큰의 사용자 ID를 조회
        Long userId = getUserIdFromMapping(refreshToken);
        if (userId != null) {
            String usedTokenKey = USED_TOKEN_PREFIX + refreshToken;
            // 🔍 사용된 토큰에 userId를 저장하여 재사용 탐지 시 전체 회수 가능하게 함
            redisTemplate.opsForValue().set(usedTokenKey, userId.toString(), 7L, TimeUnit.DAYS);
            log.debug("🏷️ 토큰을 사용됨으로 표시: {} (사용자: {})", refreshToken.substring(0, 8) + "...", userId);
        }
    }

    /**
     * 🔍 토큰이 이미 사용되었는지 확인 (재사용 탐지용)
     * 
     * @param refreshToken 확인할 토큰
     * @return 사용된 토큰이면 true, 그렇지 않으면 false
     */
    private boolean isTokenUsed(String refreshToken) {
        return getUsedTokenUserId(refreshToken) != null;
    }

    /**
     * 🔍 사용된 토큰의 원래 소유자 ID 조회 (재사용 탐지용)
     * 
     * 재사용 탐지 시 해당 사용자의 모든 토큰을 무효화하기 위해 사용됩니다.
     * 
     * @param refreshToken 조회할 토큰
     * @return 원래 소유자의 사용자 ID, 사용되지 않은 토큰이면 null
     */
    private Long getUsedTokenUserId(String refreshToken) {
        String usedTokenKey = USED_TOKEN_PREFIX + refreshToken;
        Object userIdObj = redisTemplate.opsForValue().get(usedTokenKey);
        
        if (userIdObj != null) {
            try {
                // 문자열로 저장했으므로 Long으로 변환
                return Long.parseLong(userIdObj.toString());
            } catch (NumberFormatException e) {
                log.warn("⚠️ 사용된 토큰 키 형식 오류: {} -> {}", refreshToken.substring(0, 8) + "...", userIdObj);
                return null;
            }
        }
        
        return null;
    }

    /**
     * ⏰ JWT 설정에서 Refresh Token TTL 계산 (내부용)
     * 
     * application.yml의 refresh-token-expiration(밀리초)를 
     * Redis TTL용 시간 단위로 변환합니다.
     * 
     * @return TTL 값과 시간 단위를 담은 TTL 정보 객체
     */
    private TTLInfo getRefreshTokenTTL() {
        long expirationMs = jwtProperties.getRefreshTokenExpiration();
        
        // 🧮 밀리초를 적절한 단위로 변환 (성능과 정확성 고려)
        if (expirationMs >= TimeUnit.DAYS.toMillis(1)) {
            // 1일 이상이면 일(day) 단위 사용
            long days = TimeUnit.MILLISECONDS.toDays(expirationMs);
            log.debug("⏰ Refresh Token TTL 계산: {}ms = {}일", expirationMs, days);
            return new TTLInfo(days, TimeUnit.DAYS);
            
        } else if (expirationMs >= TimeUnit.HOURS.toMillis(1)) {
            // 1시간 이상이면 시간(hour) 단위 사용
            long hours = TimeUnit.MILLISECONDS.toHours(expirationMs);
            log.debug("⏰ Refresh Token TTL 계산: {}ms = {}시간", expirationMs, hours);
            return new TTLInfo(hours, TimeUnit.HOURS);
            
        } else {
            // 그 외는 분(minute) 단위 사용
            long minutes = TimeUnit.MILLISECONDS.toMinutes(expirationMs);
            log.debug("⏰ Refresh Token TTL 계산: {}ms = {}분", expirationMs, minutes);
            return new TTLInfo(Math.max(1, minutes), TimeUnit.MINUTES); // 최소 1분 보장
        }
    }

    /**
     * ⏰ TTL 정보를 담는 내부 헬퍼 클래스
     */
    private static class TTLInfo {
        final long value;
        final TimeUnit unit;
        
        TTLInfo(long value, TimeUnit unit) {
            this.value = value;
            this.unit = unit;
        }
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