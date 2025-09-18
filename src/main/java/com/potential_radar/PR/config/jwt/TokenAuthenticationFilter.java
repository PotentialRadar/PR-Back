package com.potential_radar.PR.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 🛡️ JWT 토큰 인증 필터 - Spring Security 인증 체인의 핵심!
 * 
 * 🤔 이 필터가 하는 일:
 * - 모든 HTTP 요청을 가로채서 JWT 토큰 확인
 * - 유효한 토큰이면 Spring Security 인증 정보 설정
 * - 무효한 토큰이면 인증 없이 다음 필터로 전달
 * 
 * 🔄 필터 체인에서의 위치:
 * Client → [CORS] → [CSRF] → **[JWT Filter]** → [OAuth2] → Controller
 * 
 * 🎯 주요 기능:
 * 1️⃣ 쿠키 우선으로 토큰 추출 (XSS 방지)
 * 2️⃣ Authorization 헤더 백업 지원 (API 호환성)
 * 3️⃣ JWT 검증 및 SecurityContext 설정
 * 4️⃣ 인증 실패 시 graceful 처리 (예외 차단)
 * 
 * 🛡️ 보안 특징:
 * - HttpOnly 쿠키 우선 사용으로 XSS 공격 방지
 * - 토큰 로깅 시 부분 마스킹으로 보안 강화
 * - OncePerRequestFilter 상속으로 중복 실행 방지
 */

@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    
    // 🔑 HTTP 헤더와 토큰 관련 상수들
    private static final String HEADER_AUTHORIZATION = "Authorization";  // 표준 인증 헤더
    private static final String TOKEN_PREFIX = "Bearer ";                // JWT 토큰 접두사

    /**
     * 🔍 필터의 핵심 로직 - 모든 HTTP 요청에 대해 실행됨
     * 
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 🛡️ **JWT 인증 처리 흐름**                                   │
     * │                                                             │
     * │ 1️⃣ HTTP 요청 인터셉트                                       │
     * │      ↓                                                      │
     * │ 2️⃣ 쿠키/헤더에서 JWT 토큰 추출                             │
     * │      ├─ 🍪 access_token 쿠키 우선 확인                      │
     * │      └─ 📋 Authorization 헤더 백업 확인                     │
     * │      ↓                                                      │
     * │ 3️⃣ JWT 토큰 유효성 검증                                    │
     * │      ├─ ✅ 유효 → SecurityContext에 인증 정보 설정          │
     * │      └─ ❌ 무효 → 인증 없이 다음 필터로 전달               │
     * │      ↓                                                      │
     * │ 4️⃣ 다음 필터 체인으로 요청 전달                            │
     * └─────────────────────────────────────────────────────────────┘
     * 
     * @param request HTTP 요청 객체 (쿠키, 헤더 포함)
     * @param response HTTP 응답 객체
     * @param filterChain 다음 필터들의 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 🔍 1단계: 요청에서 JWT 토큰 추출 (쿠키 우선, 헤더 백업)
        String token = resolveToken(request);

        // 🔐 2단계: 토큰이 존재하고 유효한지 검증
        if (token != null && tokenProvider.validToken(token)) {
            try {
                // ✅ 3단계: 유효한 토큰에서 인증 정보 생성
                Authentication authentication = tokenProvider.getAuthentication(token);
                if (authentication != null) {
                    // 🛡️ Spring Security Context에 인증 정보 설정
                    // 💡 이제 이 요청은 "인증된 사용자"로 처리됨
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("✅ JWT 인증 성공: {}", authentication.getName());
                }
            } catch (Exception e) {
                // 🚨 예외 발생 시에도 필터 체인 중단하지 않음 (graceful handling)
                log.error("⚠️ JWT 인증 중 예외 발생: {}", e.getMessage(), e);
                SecurityContextHolder.clearContext(); // 혹시 모를 인증 정보 정리
            }
        } else if (token != null) {
            // ❌ 토큰은 있지만 유효하지 않은 경우
            log.error("❌ 유효하지 않은 JWT 토큰: {}", token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            // 🔍 토큰이 아예 없는 경우 (정상적인 상황, 로그인 전 등)
            log.debug("🔍 토큰이 없습니다 - 인증되지 않은 요청");
        }

        // 🔄 4단계: 인증 처리 완료 후 다음 필터로 요청 전달
        // 💡 인증 성공/실패와 관계없이 항상 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 🔍 HTTP 요청에서 JWT 토큰 추출 - 보안 우선순위 기반 전략
     * 
     * 🛡️ 토큰 추출 우선순위:
     * 1️⃣ HttpOnly 쿠키 (보안성 최우선)
     * 2️⃣ Authorization 헤더 (API 호환성 백업)
     * 
     * 🤔 왜 쿠키를 우선하나요?
     * - HttpOnly 속성으로 JavaScript 접근 차단 (XSS 방지)
     * - Secure 속성으로 HTTPS에서만 전송 (도청 방지)
     * - SameSite 속성으로 CSRF 공격 완화
     * 
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 🔍 **토큰 추출 전략**                                       │
     * │                                                             │
     * │ 1️⃣ Cookie 확인: "access_token"                             │
     * │      ├─ ✅ 존재 + 유효 → 토큰 반환                          │
     * │      └─ ❌ 없음/비어있음 → 2단계로                          │
     * │           ↓                                                │
     * │ 2️⃣ Header 확인: "Authorization: Bearer xxx"               │
     * │      ├─ ✅ 올바른 형식 → 토큰 반환                          │
     * │      └─ ❌ 없음/잘못된 형식 → null 반환                     │
     * └─────────────────────────────────────────────────────────────┘
     * 
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 문자열, 없으면 null
     */
    private String resolveToken(HttpServletRequest request) {
        
        // 🍪 1단계: HttpOnly Cookie에서 토큰 추출 (보안 최우선)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    // 🔍 빈 문자열이나 null 체크로 유효성 확인
                    if (value != null && !value.trim().isEmpty()) {
                        log.debug("🍪 쿠키에서 토큰 추출 성공");
                        return value;
                    }
                }
            }
        }

        // 📋 2단계: Authorization 헤더에서 토큰 추출 (API 호환성 백업)
        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            String token = authorizationHeader.substring(TOKEN_PREFIX.length());
            log.debug("📋 Authorization 헤더에서 토큰 추출 성공");
            return token;
        }

        // 🔍 토큰을 찾을 수 없음
        return null;
    }
}
