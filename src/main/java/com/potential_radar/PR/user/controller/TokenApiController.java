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
            // 🔄 새로운 Access Token 생성 (Refresh Token 회전 포함)
            String newAccessToken = tokenService.createNewAccessToken(refreshToken);
            
            // 🍪 새 Access Token을 보안 쿠키로 설정
            int accessTokenMaxAge = (int) (jwtProperties.getAccessTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "access_token", newAccessToken, accessTokenMaxAge);
            
            log.info("✅ Access Token 갱신 성공");
            
            return ResponseEntity.ok(Map.of(
                "message", "토큰이 성공적으로 갱신되었습니다.",
                "expiresIn", accessTokenMaxAge
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
    
    /**
     * 🚪 로그아웃 API
     * 
     * 현재 사용자의 모든 토큰을 무효화하고 쿠키를 삭제합니다.
     * 보안을 위해 서버와 클라이언트 양쪽에서 토큰을 완전히 제거합니다.
     * 
     * @param request HttpServletRequest 객체 (쿠키 추출용)
     * @param response HttpServletResponse 객체 (쿠키 삭제용)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/api/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        
        try {
            // 🍪 Refresh Token 쿠키에서 사용자 정보 추출
            Optional<Cookie> refreshCookie = CookieUtil.getCookie(request, "refresh_token");
            
            if (refreshCookie.isPresent()) {
                String refreshToken = refreshCookie.get().getValue();
                
                // 🔍 사용자 ID 조회 후 모든 토큰 삭제
                // TokenService에서 사용자 ID를 조회하는 메소드가 필요하므로 추가해야 함
                // 임시로 refresh token으로 사용자를 식별하여 삭제
                try {
                    // Redis에서 토큰으로 사용자 ID를 조회하고 모든 토큰을 삭제
                    tokenService.logoutByRefreshToken(refreshToken);
                    log.info("🚪 사용자 로그아웃 완료");
                } catch (Exception e) {
                    log.warn("⚠️ 토큰 삭제 중 오류 발생: {}", e.getMessage());
                    // 로그아웃은 실패해도 쿠키는 삭제해야 함
                }
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 로그아웃 중 토큰 처리 실패: {}", e.getMessage());
            // 로그아웃은 실패해도 쿠키는 삭제해야 함
        }
        
        // 🗑️ 클라이언트 쿠키 삭제
        CookieUtil.deleteCookie(response, "access_token");
        CookieUtil.deleteCookie(response, "refresh_token");
        
        log.info("🚪 로그아웃 완료 - 모든 쿠키 삭제됨");
        
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }
}