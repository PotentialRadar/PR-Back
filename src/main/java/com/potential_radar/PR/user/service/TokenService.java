package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.common.exception.InvalidTokenException;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🎫 토큰 관리 서비스 (Redis 기반으로 업그레이드)
 * 
 * JWT Access Token과 Refresh Token의 생성, 갱신, 삭제를 담당합니다.
 * Redis 기반의 새로운 토큰 시스템을 사용하여 보안성과 성능을 향상시켰습니다.
 * 
 * 주요 기능:
 * - Access Token 갱신 (Refresh Token 회전 포함)
 * - 사용자별 모든 토큰 삭제 (로그아웃)
 * - 토큰 재사용 탐지 및 보안 위반 처리
 * 
 * 보안 개선사항:
 * - Refresh Token 자동 회전으로 탈취 위험 감소
 * - Redis TTL로 자동 만료 처리
 * - 재사용 탐지 시 모든 토큰 즉시 무효화
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TokenService {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;  // 순환 의존성 해결: UserService 대신 직접 Repository 사용
    private final RedisRefreshTokenService redisRefreshTokenService;

    /**
     * 🔄 Access Token 갱신 및 Refresh Token 회전
     * 
     * 기존 Refresh Token을 사용하여 새로운 Access Token을 발급하고,
     * 보안을 위해 Refresh Token도 새것으로 회전(교체)합니다.
     * 
     * @param refreshToken 기존 Refresh Token
     * @return 새로 발급된 Access Token
     * @throws InvalidTokenException 토큰이 유효하지 않거나 재사용 탐지 시
     * @deprecated 새 Refresh Token도 함께 반환하는 refreshTokenPair() 메소드 사용 권장
     */
    @Deprecated
    @Transactional
    public String createNewAccessToken(String refreshToken) {
        TokenPair tokenPair = refreshTokenPair(refreshToken);
        return tokenPair.getAccessToken();
    }

    /**
     * 🔄 토큰 쌍 갱신 (Access + Refresh Token 회전)
     * 
     * 기존 Refresh Token을 사용하여 새로운 토큰 쌍을 발급합니다.
     * 보안을 위해 Refresh Token도 새것으로 회전(교체)합니다.
     * 
     * ⚠️ 중요: 반환된 새 Refresh Token을 반드시 클라이언트에 전달해야 합니다!
     * 
     * @param refreshToken 기존 Refresh Token
     * @return 새로 발급된 토큰 쌍 (Access Token + 회전된 Refresh Token)
     * @throws InvalidTokenException 토큰이 유효하지 않거나 재사용 탐지 시
     */
    @Transactional
    public TokenPair refreshTokenPair(String refreshToken) {
        // 🔍 Refresh Token 유효성 검사 및 사용자 조회
        Long userId = redisRefreshTokenService.getUserIdByToken(refreshToken);
        
        if (userId == null) {
            log.warn("❌ 유효하지 않은 Refresh Token으로 갱신 요청: {}", 
                refreshToken.substring(0, 8) + "...");
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
        }

        // 👤 사용자 정보 조회 (Repository 직접 사용으로 순환 의존성 해결)
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new InvalidTokenException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 🔄 Refresh Token 회전 (보안 강화)
        String newRefreshToken = redisRefreshTokenService.rotateRefreshToken(refreshToken);
        
        if (newRefreshToken == null) {
            log.error("🔥 Refresh Token 회전 실패 - 사용자: {}", userId);
            throw new InvalidTokenException("토큰 갱신에 실패했습니다. 다시 로그인해주세요.");
        }

        // 🎫 새로운 Access Token 생성
        String newAccessToken = tokenProvider.generateAccessToken(user);
        
        log.info("✅ 토큰 쌍 갱신 성공 - 사용자: {}, 새 Refresh Token: {}", 
            userId, newRefreshToken.substring(0, 8) + "...");
            
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * 🎫 새로운 토큰 쌍 생성 (로그인 시 사용)
     * 
     * 로그인 성공 시 Access Token과 Refresh Token을 새로 생성합니다.
     * 
     * @param user 로그인한 사용자 정보
     * @return TokenPair 객체 (Access Token, Refresh Token 포함)
     */
    @Transactional
    public TokenPair createTokenPair(User user) {
        // 🎫 Access Token 생성
        String accessToken = tokenProvider.generateAccessToken(user);
        
        // 🔄 Refresh Token 생성
        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getUserId());
        
        log.info("🎫 새 토큰 쌍 생성 완료 - 사용자: {}", user.getUserId());
        
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 🗑️ 사용자의 모든 Refresh Token 삭제 (로그아웃)
     * 
     * 로그아웃 시 또는 보안 위반 탐지 시 해당 사용자의 모든 토큰을 무효화합니다.
     * 
     * @param userId 토큰을 삭제할 사용자 ID
     */
    @Transactional
    public void deleteRefreshToken(Long userId) {
        redisRefreshTokenService.revokeAllTokensForUser(userId);
        log.info("🗑️ 사용자 {} 로그아웃 - 모든 Refresh Token 삭제됨", userId);
    }

    /**
     * ✅ Refresh Token 유효성 검증 (관리용)
     * 
     * @param refreshToken 검증할 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidRefreshToken(String refreshToken) {
        return redisRefreshTokenService.isValidToken(refreshToken);
    }

    /**
     * 🔍 Refresh Token으로 사용자 ID 조회
     * 
     * @param refreshToken 조회할 토큰
     * @return 사용자 ID, 유효하지 않으면 null
     */
    public Long getUserIdByRefreshToken(String refreshToken) {
        return redisRefreshTokenService.getUserIdByToken(refreshToken);
    }

    /**
     * 🚪 Refresh Token으로 로그아웃
     * 
     * @param refreshToken 로그아웃할 사용자의 토큰
     */
    @Transactional
    public void logoutByRefreshToken(String refreshToken) {
        Long userId = redisRefreshTokenService.getUserIdByToken(refreshToken);
        if (userId != null) {
            redisRefreshTokenService.revokeAllTokensForUser(userId);
            log.info("🚪 토큰으로 로그아웃 완료 - 사용자: {}", userId);
        }
    }

    /**
     * 📦 토큰 쌍을 담는 데이터 클래스
     * 
     * Access Token과 Refresh Token을 함께 반환할 때 사용합니다.
     * 로그인 성공 시 프론트엔드에 전달됩니다.
     */
    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class TokenPair {
        private final String accessToken;   // 🎫 30분 만료 Access Token
        private final String refreshToken;  // 🔄 14일 만료 Refresh Token
    }
}
