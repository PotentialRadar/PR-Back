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
 * 📌 역할:
 * JWT가 유효하면 SecurityContextHolder에 인증 정보 저장
 */

@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request); // ✅ 헤더 or 쿠키에서 토큰 추출

        if (token != null && tokenProvider.validToken(token)) {
            try {
                Authentication authentication = tokenProvider.getAuthentication(token);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("✅ JWT 인증 성공: {}", authentication.getName());
                }
            } catch (Exception e) {
                log.error("⚠️ JWT 인증 중 예외 발생: {}", e.getMessage(), e);
            }
        } else if (token != null) {
            log.error("❌ 유효하지 않은 JWT 토큰: {}", token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            log.debug("🔍 토큰이 없습니다");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청에서 JWT 토큰 추출 (쿠키 우선 → 헤더 보조)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. HttpOnly Cookie (우선순위 높음)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Authorization 헤더 (하위 호환성을 위해 유지)
        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_PREFIX.length());
        }

        return null;
    }
}
