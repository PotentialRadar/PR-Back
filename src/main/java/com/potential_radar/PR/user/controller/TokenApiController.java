package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.config.jwt.JwtProperties;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.Optional;

/**
 * 🎫 토큰 관리 API 컨트롤러 (Redis + 쿠키 기반으로 업그레이드)
 * 
 * JWT Access Token과 Refresh Token의 갱신을 담당하는 REST API 컨트롤러입니다.
 * 기존 요청 본문 방식 대신 HttpOnly 쿠키를 사용하여 보안성을 향상시켰습니다.
 * 
 * 주요 기능:
 * - Refresh Token 쿠키를 사용하여 새로운 Access Token 발급
 * - 자동 토큰 회전으로 보안 강화
 * - Refresh Token 재사용 탐지 및 무효화
 * - 만료된 Access Token을 새로고침 없이 재발급
 * 
 * 보안 개선사항:
 * - HttpOnly 쿠키로 XSS 공격 방지
 * - Refresh Token 자동 회전으로 탈취 위험 감소
 * - Redis 기반 토큰 관리로 성능 향상
 * - 재사용 탐지 시 모든 토큰 즉시 무효화
 * 
 * API 엔드포인트:
 * POST /api/token - Refresh Token으로 새 Access Token 발급
 * POST /api/logout - 모든 토큰 무효화 및 로그아웃
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class TokenApiController {
    
    // 🔧 의존성 주입받는 서비스들
    private final TokenService tokenService;       // 토큰 관리 비즈니스 로직
    private final JwtProperties jwtProperties;      // JWT 설정 정보

    /**
     * 🔄 새로운 Access Token 발급 API (토큰 회전 포함)
     * 
     * HttpOnly 쿠키의 Refresh Token을 사용하여 새로운 Access Token을 발급하고,
     * 보안 강화를 위해 Refresh Token도 새것으로 회전(교체)합니다.
     * 
     * 처리 과정:
     * 1. 쿠키에서 Refresh Token 추출
     * 2. Refresh Token 유효성 검증 및 재사용 탐지
     * 3. 기존 Refresh Token을 새것으로 회전
     * 4. 새로운 Access Token 생성
     * 5. 새 토큰들을 HttpOnly 쿠키로 설정하여 반환
     * 
     * 보안 특징:
     * - 토큰 회전으로 탈취 위험 감소
     * - 재사용 탐지 시 모든 토큰 즉시 무효화
     * - HttpOnly 쿠키로 XSS 공격 방지
     * 
     * @param request HttpServletRequest 객체 (쿠키 추출용)
     * @param response HttpServletResponse 객체 (쿠키 설정용)
     * @return 성공 메시지를 포함하는 응답 객체
     */
    @PostMapping("/api/token")
    public ResponseEntity<Object> createNewAccessToken(HttpServletRequest request, HttpServletResponse response) {
        
        // 🍪 쿠키에서 Refresh Token 추출
        Optional<Cookie> refreshCookie = CookieUtil.getCookie(request, "refresh_token");
        
        if (refreshCookie.isEmpty()) {
            log.warn("❌ Refresh Token 쿠키가 없습니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다. 다시 로그인해주세요."));
        }
        
        String refreshToken = refreshCookie.get().getValue();
        
        try {
            // 🔄 **능동 대응 토큰 시스템 실행**
            // ┌─────────────────────────────────────────────────────────────────┐
            // │  🛡️ 보안 흐름: 기존 토큰 → 새 토큰 쌍으로 완전 교체                       │
            // │  1️⃣ 기존 Refresh Token 검증                                       │
            // │  2️⃣ 재사용 토큰 감지 (공격자 탐지)                                    │
            // │  3️⃣ 기존 토큰을 used_token 리스트에 기록                              │
            // │  4️⃣ 완전히 새로운 토큰 쌍 생성                                        │
            // │  5️⃣ 클라이언트에 새 토큰 전달 (쿠키)                                   │
            // └─────────────────────────────────────────────────────────────────┘
            TokenService.TokenPair tokenPair = tokenService.refreshTokenPair(refreshToken);
            
            // 🍪 새 Access Token을 보안 쿠키로 설정 (30분 만료)
            int accessTokenMaxAge = (int) (jwtProperties.getAccessTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "access_token", tokenPair.getAccessToken(), accessTokenMaxAge);
            
            // 🍪 ⚠️ **중요: 새 Refresh Token도 반드시 쿠키로 설정** 
            // 💡 기존 토큰은 이미 "사용됨" 처리되어 재사용 시 공격 감지됨!
            int refreshTokenMaxAge = (int) (jwtProperties.getRefreshTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "refresh_token", tokenPair.getRefreshToken(), refreshTokenMaxAge);
            
            log.info("✅ 능동 대응 토큰 회전 완료 - 기존 토큰 무효화 및 새 토큰 쌍 발급");
            
            return ResponseEntity.ok(Map.of(
                "message", "토큰이 성공적으로 갱신되었습니다.",
                "accessExpiresIn", accessTokenMaxAge,
                "refreshExpiresIn", refreshTokenMaxAge
            ));
            
        } catch (Exception e) {
            log.error("🔥 토큰 갱신 실패: {}", e.getMessage());
            
            // 🗑️ 실패 시 쿠키 정리
            CookieUtil.deleteCookie(response, "access_token");
            CookieUtil.deleteCookie(response, "refresh_token");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 갱신에 실패했습니다. 다시 로그인해주세요."));
        }
    }
    
}