package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.JwtProperties;
import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.common.exception.InvalidTokenException;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.token.RefreshTokenStore;
import com.potential_radar.PR.user.service.token.RotateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * 🎫 토큰 관리 서비스
 *
 * JWT Access Token과 Refresh Token의 생성, 갱신, 삭제를 담당합니다.
 * Refresh Token 저장은 RefreshTokenStore(기본: MigratingRefreshTokenStore)에 위임하며,
 * auth.token.* 설정으로 DB / Redis 저장소 사이를 전환할 수 있습니다.
 *
 * 주요 기능:
 * - Access Token 갱신 (Refresh Token 회전 포함, Redis에서는 Lua 스크립트로 원자적 처리)
 * - 사용자별 모든 토큰 삭제 (로그아웃)
 * - 토큰 재사용 탐지 시 해당 사용자의 모든 토큰 즉시 무효화
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TokenService {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;  // 순환 의존성 방지: UserService 대신 Repository 직접 사용
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;

    @Deprecated
    @Transactional
    public String createNewAccessToken(String refreshToken) {
        return refreshTokenPair(refreshToken).getAccessToken();
    }

    /**
     * Refresh Token으로 새 토큰 쌍을 발급하고 기존 Refresh Token을 회전(무효화)합니다.
     *
     * noRollbackFor: 재사용 탐지로 토큰을 무효화한 뒤 InvalidTokenException을 던지는데,
     * 이때 롤백되면 DB 쪽 삭제가 취소되어 2단계(DB 폴백)에서 탈취 토큰이 다시 통과할 수 있다.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public TokenPair refreshTokenPair(String refreshToken) {
        // 이미 회전된 토큰이어도 소유자를 찾는다. 재사용인지 동시 요청인지는 rotate()가 원자적으로 판정한다.
        Long userId = refreshTokenStore.findOwnerForRotation(refreshToken)
                .orElseThrow(() -> {
                    log.warn("❌ 토큰 검증 실패 (무효하거나 만료됨): {}", mask(refreshToken));
                    return new InvalidTokenException("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("사용자를 찾을 수 없습니다. ID: " + userId));

        RotateResult result = refreshTokenStore.rotate(userId, refreshToken, newTokenValue(), refreshTtl());
        switch (result.outcome()) {
            case ROTATED -> log.info("✅ 토큰 회전 성공 - 사용자: {}", userId);
            // 유예 안에 다시 온 직전 토큰: 동시 요청으로 보고 방금 발급한 토큰을 그대로 돌려준다
            case GRACE_HIT -> log.info("[TOKEN_GRACE] 유예 구간 내 동시 재발급. userId={} token={}", userId, mask(refreshToken));
            case REUSE_DETECTED -> {
                log.warn("[TOKEN_REUSE] 회전된 토큰 재사용 감지. userId={} token={} 의 모든 토큰을 무효화합니다",
                        userId, mask(refreshToken));
                refreshTokenStore.deleteAllForUser(userId);
                throw new InvalidTokenException("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
            }
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        return new TokenPair(newAccessToken, result.refreshToken());
    }

    @Transactional
    public TokenPair createTokenPair(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user.getUserId());
        log.info("🎫 새 토큰 쌍 생성 완료 - 사용자: {}", user.getUserId());
        return new TokenPair(accessToken, refreshToken);
    }

    /** 새 Refresh Token을 발급해 저장합니다. 기존 토큰은 대체됩니다. */
    @Transactional
    public String issueRefreshToken(Long userId) {
        String refreshToken = newTokenValue();
        refreshTokenStore.save(userId, refreshToken, refreshTtl());
        return refreshToken;
    }

    @Transactional
    public void deleteRefreshToken(Long userId) {
        refreshTokenStore.deleteAllForUser(userId);
        log.info("🗑️ 사용자 {} 로그아웃 - 모든 Refresh Token 삭제됨", userId);
    }

    public boolean isValidRefreshToken(String refreshToken) {
        return refreshTokenStore.findUserIdByToken(refreshToken).isPresent();
    }

    public Long getUserIdByRefreshToken(String refreshToken) {
        return refreshTokenStore.findUserIdByToken(refreshToken).orElse(null);
    }

    @Transactional
    public void logoutByRefreshToken(String refreshToken) {
        refreshTokenStore.findUserIdByToken(refreshToken).ifPresent(userId -> {
            refreshTokenStore.deleteAllForUser(userId);
            log.info("🚪 토큰으로 로그아웃 완료 - 사용자: {}", userId);
        });
    }

    private Duration refreshTtl() {
        return Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());
    }

    private static String newTokenValue() {
        return UUID.randomUUID().toString();
    }

    private static String mask(String token) {
        return token.length() > 8 ? token.substring(0, 8) + "..." : "***";
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class TokenPair {
        private final String accessToken;   // 🎫 Access Token
        private final String refreshToken;  // 🔄 Refresh Token
    }
}
