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
    
    // ┌─────────────────────────────────────────────────────────────────┐
    // │ 🔑 **Redis 키 네이밍 전략** - 체계적인 데이터 관리               │
    // │                                                                 │
    // │ 🤔 왜 접두사를 사용하나요?                                       │
    // │ - Redis는 하나의 거대한 저장소 → 데이터 구분 필요                │
    // │ - 접두사로 데이터 종류를 구분 (마치 폴더 구조와 같음)             │
    // │ - 관리와 디버깅이 쉬워짐                                         │
    // └─────────────────────────────────────────────────────────────────┘
    
    // 🔑 **1️⃣ 메인 토큰 저장소** - 사용자별 토큰을 구분하는 네임스페이스
    // 💾 저장 형태: "refresh_token:123" → RefreshTokenInfo 객체
    // 📋 용도: 사용자 ID로 해당 사용자의 활성 토큰 정보 조회
    // 🕒 TTL: 14일 (Refresh Token 만료 시간과 동일)
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    // 🔄 **2️⃣ 역방향 매핑 테이블** - 토큰으로 사용자 ID를 조회하는 고속 검색용
    // 💾 저장 형태: "token_mapping:abc-def-ghi" → 123 (사용자 ID)
    // 📋 용도: 토큰 값으로 빠르게 소유자 찾기 (인증 시 필수)
    // 🕒 TTL: 14일 (메인 토큰과 동기화)
    // ⚡ 성능: O(1) 시간복잡도로 즉시 조회 가능
    private static final String TOKEN_MAPPING_PREFIX = "token_mapping:";
    
    // 🚨 **3️⃣ 블랙리스트** - 재사용 탐지를 위한 사용된 토큰 추적소
    // 💾 저장 형태: "used_token:abc-def-ghi" → "123" (원래 소유자 ID)
    // 📋 용도: 토큰 재사용 공격 감지 및 능동 대응
    // 🕒 TTL: 7일 (충분한 탐지 기간 보장)
    // 🛡️ 보안: 재사용 감지 시 해당 사용자 모든 세션 무효화
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
        
        // 💾 **1단계: 메인 토큰 정보 저장** (사용자 ID → 토큰 정보)
        // ┌─────────────────────────────────────────────────────────────┐
        // │ 🔧 **RedisTemplate 사용법 예시**                            │
        // │                                                             │
        // │ redisTemplate.opsForValue().set(                           │
        // │   key,        // 🔑 Redis 키 (문자열)                       │
        // │   value,      // 📦 저장할 값 (객체)                        │
        // │   ttl,        // ⏰ 수명 (숫자)                              │
        // │   timeUnit    // 🕒 시간 단위 (DAYS/HOURS/MINUTES)          │
        // │ );                                                          │
        // └─────────────────────────────────────────────────────────────┘
        String userKey = REFRESH_TOKEN_PREFIX + userId;  // "refresh_token:123"
        redisTemplate.opsForValue().set(
            userKey,           // 🔑 Key: "refresh_token:123"
            tokenInfo,         // 📦 Value: RefreshTokenInfo 객체 (JSON으로 자동 변환)
            ttlInfo.value,     // ⏰ TTL: 14 (숫자)
            ttlInfo.unit       // 🕒 Unit: TimeUnit.DAYS
        );
        
        // 🔄 **2단계: 역방향 매핑 테이블 저장** (토큰 → 사용자 ID)
        String tokenKey = TOKEN_MAPPING_PREFIX + refreshToken;  // "token_mapping:abc-def-ghi"
        redisTemplate.opsForValue().set(
            tokenKey,          // 🔑 Key: "token_mapping:abc-def-ghi" 
            userId,            // 📦 Value: 123 (Long 타입, JSON으로 자동 변환)
            ttlInfo.value,     // ⏰ TTL: 14 (메인 토큰과 동일)
            ttlInfo.unit       // 🕒 Unit: TimeUnit.DAYS
        );
        
        log.info("🎫 사용자 {}의 새 Refresh Token 생성됨: {}", userId, refreshToken.substring(0, 8) + "...");
        return refreshToken;
    }

    /**
     * 🔍 Refresh Token으로 사용자 ID 조회 - ⭐ 재사용 감지 시스템의 최전선!
     * 
     * 🛡️ 비유: 출입증을 확인하는 보안 게이트와 같습니다
     * 
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 🚨 **능동 대응 보안 흐름도**                               │
     * │                                                             │
     * │ 1️⃣ 토큰 접수                                               │
     * │      ↓                                                      │
     * │ 2️⃣ used_token 블랙리스트 확인                              │
     * │      ├─ 🔴 발견됨 → 재사용 공격 감지!                      │
     * │      │   ├─ ⚡ 즉시 revokeAllTokensForUser() 호출          │
     * │      │   ├─ 🛡️ 해당 사용자 모든 세션 강제 종료             │
     * │      │   └─ ❌ 접근 거부 (null 반환)                       │
     * │      └─ 🟢 깨끗함 → 정상 처리 진행                         │
     * │           ↓                                                │
     * │ 3️⃣ token_mapping에서 사용자 ID 조회                       │
     * │      └─ ✅ 사용자 ID 반환                                  │
     * └─────────────────────────────────────────────────────────────┘
     * 
     * 🚨 능동 대응의 핵심!
     * - 공격자가 탈취한 토큰으로 접근 시도하면 **즉시 감지**
     * - 피해자의 **모든 세션을 강제 종료**하여 추가 피해 방지
     * 
     * @param refreshToken 조회할 Refresh Token 문자열
     * @return 토큰 소유자의 사용자 ID, 유효하지 않으면 null
     */
    public Long getUserIdByToken(String refreshToken) {
        // 🚨 1단계: 재사용 탐지 - 가장 중요한 보안 검사!
        Long usedTokenUserId = getUsedTokenUserId(refreshToken);
        if (usedTokenUserId != null) {
            // 🚨 경보 발생! 이미 사용된 토큰으로 재접근 시도 감지!
            log.warn("⚠️ 🚨 토큰 재사용 공격 감지! 토큰: {} (원 소유자: {})", 
                refreshToken.substring(0, 8) + "...", usedTokenUserId);
            
            // ⚡ 즉시 대응: 해당 사용자의 모든 토큰 무효화 (능동 대응!)
            // 💡 왜 이렇게 하나요? 공격자가 토큰을 탈취했을 가능성이 높으므로
            //     피해자를 보호하기 위해 모든 세션을 강제 종료시킵니다.
            revokeAllTokensForUser(usedTokenUserId);
            
            log.info("🛡️ 보안 대응 완료: 사용자 {}의 모든 토큰이 무효화되었습니다", usedTokenUserId);
            return null;  // 접근 거부
        }
        
        // ✅ 2단계: 정상 토큰 - 사용자 ID 조회 후 반환
        return getUserIdFromMapping(refreshToken);
    }

    /**
     * 🔄 Refresh Token 회전 (사용 후 새 토큰 발급) - ⭐ 능동 대응의 핵심!
     * 
     * 🏠 비유: 집 열쇠를 한 번 사용할 때마다 자물쇠를 바꾸는 것과 같습니다
     * 
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 🔄 **토큰 회전 흐름도** (능동 대응의 핵심!)                 │
     * │                                                             │
     * │ 1️⃣ 기존 토큰 검증                                          │
     * │      ├─ ❌ 무효 → null 반환 (회전 실패)                     │
     * │      └─ ✅ 유효 → 2단계 진행                                │
     * │           ↓                                                │
     * │ 2️⃣ 기존 토큰을 used_token 블랙리스트에 추가                │
     * │      └─ 🏷️ markTokenAsUsed() 호출                          │
     * │           ↓                                                │
     * │ 3️⃣ 완전히 새로운 토큰 생성                                  │
     * │      └─ 🎫 createRefreshToken() 호출                       │
     * │           ↓                                                │
     * │ 4️⃣ **결과: 기존 토큰 재사용 시 공격자 즉시 탐지됨!**        │
     * └─────────────────────────────────────────────────────────────┘
     * 
     * 🛡️ 보안 효과:
     * - 토큰이 탈취되어도 한 번 사용하면 무효화됨
     * - 공격자가 탈취한 토큰으로 재접근 시도 시 즉시 감지 가능
     * 
     * @param oldToken 사용된 기존 토큰 (이제 폐기될 토큰)
     * @return 새로 발급된 토큰, 실패 시 null
     */
    public String rotateRefreshToken(String oldToken) {
        // 🔍 1단계: 기존 토큰이 유효한 토큰인지 확인
        Long userId = getUserIdFromMapping(oldToken);
        if (userId == null) {
            log.warn("❌ 유효하지 않은 토큰으로 회전 시도: {}", oldToken.substring(0, 8) + "...");
            return null;
        }
        
        // 🏷️ 2단계: 기존 토큰을 "사용됨" 블랙리스트에 추가 (능동 대응의 핵심!)
        // 💡 이제 이 토큰을 누군가 다시 사용하려고 하면 바로 감지됨!
        markTokenAsUsed(oldToken);
        
        // 🎫 3단계: 완전히 새로운 토큰 생성 및 발급
        String newToken = createRefreshToken(userId);
        log.info("🔄 사용자 {}의 토큰 회전 완료: {} → {}", 
            userId, oldToken.substring(0, 8) + "...", newToken.substring(0, 8) + "...");
        
        return newToken;
    }

    /**
     * 🗑️ 특정 사용자의 모든 Refresh Token 무효화 - ⚡ 비상 대응의 핵심!
     * 
     * 🚨 비유: 화재 경보가 울리면 모든 출입문을 즉시 잠그는 것과 같습니다
     * 
     * 🛡️ 언제 사용되나요?
     * 1️⃣ 토큰 재사용 감지 시 → 공격자로부터 사용자 보호
     * 2️⃣ 로그아웃 시 → 정상적인 세션 종료
     * 3️⃣ 보안 위반 감지 시 → 추가 피해 방지
     * 
     * ✨ 무효화 과정:
     * 1️⃣ 현재 활성 토큰 조회
     * 2️⃣ 토큰 매핑 테이블에서 삭제 (더 이상 조회 안됨)
     * 3️⃣ 블랙리스트에 추가 (재사용 감지용)
     * 4️⃣ 사용자 토큰 정보 완전 삭제
     * 
     * 🔥 결과: 해당 사용자는 **모든 디바이스에서 강제 로그아웃**됨
     * 
     * @param userId 토큰을 무효화할 사용자 ID
     */
    public void revokeAllTokensForUser(Long userId) {
        // 🔍 1단계: 현재 이 사용자가 가진 활성 토큰 찾기
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        RefreshTokenInfo tokenInfo = (RefreshTokenInfo) redisTemplate.opsForValue().get(userKey);
        
        if (tokenInfo != null) {
            // 🗑️ 2단계: 토큰 매핑 삭제 (더 이상 이 토큰으로 조회 안됨)
            String tokenKey = TOKEN_MAPPING_PREFIX + tokenInfo.getToken();
            redisTemplate.delete(tokenKey);
            
            // 🏷️ 3단계: 블랙리스트에 추가 (혹시 누군가 이 토큰을 재사용하려 하면 감지됨)
            markTokenAsUsed(tokenInfo.getToken());
            
            log.debug("🔥 토큰 매핑 및 블랙리스트 처리 완료: {}", tokenInfo.getToken().substring(0, 8) + "...");
        }
        
        // 🗑️ 4단계: 사용자 토큰 정보 완전 삭제 (더 이상 새 토큰 발급 불가)
        redisTemplate.delete(userKey);
        
        log.info("🛡️ 보안 무효화 완료: 사용자 {}의 모든 Refresh Token이 무효화되었습니다", userId);
        log.info("📱 결과: 해당 사용자는 모든 디바이스에서 강제 로그아웃됩니다");
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
     * 🏷️ 토큰을 "사용됨" 블랙리스트에 추가 - ⭐ 능동 대응의 핵심 메커니즘!
     * 
     * 📋 비유: 사용한 쿠폰을 "사용완료" 도장 찍어서 보관하는 것과 같습니다
     * 
     * ✨ 작동 원리:
     * 1️⃣ Redis에 `used_token:토큰값` 키로 저장
     * 2️⃣ 값으로는 원래 소유자의 userId를 저장 (중요!)
     * 3️⃣ 7일간 보관 (토큰 완전 만료까지 충분한 기간)
     * 
     * 🚨 왜 userId를 함께 저장하나요?
     * - 나중에 이 토큰이 재사용되면, 누구의 토큰이었는지 알아야
     * - 그 사용자의 **모든 토큰을 무효화** 할 수 있기 때문!
     * 
     * @param refreshToken 블랙리스트에 추가할 토큰
     */
    private void markTokenAsUsed(String refreshToken) {
        // 🔍 1단계: 이 토큰의 원래 주인이 누구인지 조회
        Long userId = getUserIdFromMapping(refreshToken);
        if (userId != null) {
            // 🗂️ 2단계: Redis 블랙리스트에 추가
            String usedTokenKey = USED_TOKEN_PREFIX + refreshToken;  // "used_token:abc-123-def..."
            
            // 💾 3단계: 토큰과 함께 소유자 ID도 저장 (재사용 탐지 시 전체 무효화용)
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
     * 🔍 블랙리스트에서 사용된 토큰의 원래 소유자 찾기 - ⭐ 재사용 탐지의 핵심!
     * 
     * 🕵️ 비유: 사용된 쿠폰을 확인해서 "이거 누가 원래 쓰던 쿠폰이지?" 찾는 것
     * 
     * ✨ 작동 과정:
     * 1️⃣ Redis 블랙리스트에서 `used_token:토큰값` 조회
     * 2️⃣ 값이 있으면 → "이 토큰은 이미 사용됨!" 
     * 3️⃣ 저장된 userId 반환 → 이 사람의 모든 토큰을 무효화할 예정
     * 
     * 🚨 이 정보가 왜 중요한가요?
     * - 재사용된 토큰의 **원래 주인**을 찾아야
     * - 그 사람의 **모든 세션을 강제 종료** 할 수 있기 때문!
     * 
     * @param refreshToken 조회할 토큰 (재사용 의심 토큰)
     * @return 원래 소유자의 사용자 ID, 사용되지 않은 토큰이면 null
     */
    private Long getUsedTokenUserId(String refreshToken) {
        // 🔍 1단계: Redis 블랙리스트에서 조회
        String usedTokenKey = USED_TOKEN_PREFIX + refreshToken;  // "used_token:abc-123-def..."
        Object userIdObj = redisTemplate.opsForValue().get(usedTokenKey);
        
        if (userIdObj != null) {
            // 📖 2단계: 찾았다! 이 토큰은 이미 사용된 토큰임
            try {
                // 문자열로 저장했으므로 Long으로 변환
                Long originalOwnerId = Long.parseLong(userIdObj.toString());
                log.debug("🔍 사용된 토큰 발견: {} (원래 소유자: {})", 
                    refreshToken.substring(0, 8) + "...", originalOwnerId);
                return originalOwnerId;
                
            } catch (NumberFormatException e) {
                log.warn("⚠️ 사용된 토큰 키 형식 오류: {} -> {}", refreshToken.substring(0, 8) + "...", userIdObj);
                return null;
            }
        }
        
        // ✅ 블랙리스트에 없음 = 아직 사용되지 않은 깨끗한 토큰
        return null;
    }

    /**
     * ⏰ JWT 설정에서 Refresh Token TTL 계산 (내부용)
     * 
     * 🤔 TTL(Time To Live)이란?
     * - 데이터의 "수명"을 설정하는 기능
     * - 설정된 시간이 지나면 Redis가 자동으로 데이터 삭제
     * - 마치 음식의 유통기한과 같은 개념!
     * 
     * 🎯 우리 시스템의 TTL 전략:
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 📅 **TTL 설정 계층**                                        │
     * │                                                             │
     * │ 1️⃣ application.yml에서 기본값 설정                          │
     * │    └─ refresh-token-expiration: 1209600000 (14일)          │
     * │                                                             │
     * │ 2️⃣ Redis TTL로 자동 변환                                   │
     * │    ├─ 14일 = 14 (DAYS 단위)                                │
     * │    ├─ 2시간 = 2 (HOURS 단위)                               │
     * │    └─ 30분 = 30 (MINUTES 단위)                             │
     * │                                                             │
     * │ 3️⃣ Redis가 자동 관리                                       │
     * │    └─ 시간 만료 시 데이터 자동 삭제                          │
     * └─────────────────────────────────────────────────────────────┘
     * 
     * @return TTL 값과 시간 단위를 담은 TTL 정보 객체
     */
    private TTLInfo getRefreshTokenTTL() {
        // 📋 1단계: application.yml에서 만료 시간 가져오기
        // 💡 예: 1209600000ms = 14일
        long expirationMs = jwtProperties.getRefreshTokenExpiration();
        
        // 🧮 2단계: 밀리초를 적절한 단위로 변환 (성능과 정확성 고려)
        // 💡 Redis는 DAYS, HOURS, MINUTES 등 단위별로 최적화됨
        
        if (expirationMs >= TimeUnit.DAYS.toMillis(1)) {
            // 📅 1일 이상이면 일(day) 단위 사용
            // 💡 14일 = 14 DAYS (메모리 효율적)
            long days = TimeUnit.MILLISECONDS.toDays(expirationMs);
            log.debug("⏰ Refresh Token TTL 계산: {}ms = {}일", expirationMs, days);
            return new TTLInfo(days, TimeUnit.DAYS);
            
        } else if (expirationMs >= TimeUnit.HOURS.toMillis(1)) {
            // 🕐 1시간 이상이면 시간(hour) 단위 사용
            // 💡 2시간 = 2 HOURS
            long hours = TimeUnit.MILLISECONDS.toHours(expirationMs);
            log.debug("⏰ Refresh Token TTL 계산: {}ms = {}시간", expirationMs, hours);
            return new TTLInfo(hours, TimeUnit.HOURS);
            
        } else {
            // ⏱️ 그 외는 분(minute) 단위 사용
            // 💡 30분 = 30 MINUTES (최소 1분 보장)
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