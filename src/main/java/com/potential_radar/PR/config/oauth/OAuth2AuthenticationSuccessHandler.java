package com.potential_radar.PR.config.oauth;

import com.potential_radar.PR.config.jwt.JwtProperties;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.TokenService;
import com.potential_radar.PR.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 🎉 OAuth2 로그인 성공 핸들러 (Redis + 쿠키 기반으로 업그레이드)
 * 
 * Google, Kakao 등 OAuth2 소셜 로그인 성공 시 JWT 토큰을 생성하고
 * HttpOnly 쿠키로 설정하여 프론트엔드로 리다이렉트하는 핸들러입니다.
 * 
 * 주요 기능:
 * - OAuth2 인증 성공 시 사용자 정보 추출
 * - Redis 기반 토큰 쌍(Access + Refresh) 생성
 * - HttpOnly 보안 쿠키로 토큰 설정
 * - 프론트엔드 콜백 URL로 리다이렉트
 * 
 * 보안 개선사항:
 * - 기존 DB 기반에서 Redis 기반 토큰 관리로 전환
 * - 토큰 회전 지원으로 보안 강화
 * - HttpOnly 쿠키로 XSS 공격 방지
 * - 토큰 재사용 탐지 및 자동 무효화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // 🔧 의존성 주입받는 서비스들
    private final TokenService tokenService;          // Redis 기반 토큰 관리 서비스
    private final UserRepository userRepository;      // 사용자 정보 조회
    private final JwtProperties jwtProperties;        // JWT 설정 정보

    @Value("${app.frontend-redirect-url}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof DefaultOAuth2User)) {
            log.error("❌ OAuth2User가 아닙니다: {}", principal.getClass().getName());
            response.sendRedirect("/api/login/fail");
            return;
        }

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
        String email = extractEmail(oAuth2User, getProviderFromRequest(request));

        if (email == null) {
            log.error("❌ OAuth2 로그인은 성공했지만 email이 없습니다.");
            response.sendRedirect("/api/login/fail");
            return;
        }

        try {
            // 👤 DB에서 유저 조회 (CustomOAuth2UserService에서 이미 생성했어야 함)
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("❌ OAuth2 로그인 후 유저를 찾을 수 없습니다: {}", email);
                        return new OAuth2AuthenticationException("OAuth2 로그인 처리 중 오류가 발생했습니다.");
                    });

            // 🎫 Redis 기반 토큰 쌍 생성 (Access + Refresh Token)
            TokenService.TokenPair tokenPair = tokenService.createTokenPair(user);

            // 🍪 Access Token을 HttpOnly 보안 쿠키로 설정
            int accessTokenMaxAge = (int) (jwtProperties.getAccessTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "access_token", tokenPair.getAccessToken(), accessTokenMaxAge);

            // 🍪 Refresh Token을 HttpOnly 보안 쿠키로 설정
            int refreshTokenMaxAge = (int) (jwtProperties.getRefreshTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "refresh_token", tokenPair.getRefreshToken(), refreshTokenMaxAge);

            // 📍 프론트엔드로 리다이렉트 (토큰은 쿠키로 전달됨)
            response.sendRedirect(frontendCallbackUrl);

            log.info("✅ OAuth2 로그인 성공. Redis 토큰 발급 완료: {}", email);

        } catch (Exception e) {
            log.error("🔥 OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            response.sendRedirect("/api/login/fail");
        }
    }

    private String extractEmail(DefaultOAuth2User oAuth2User, String provider) {
        if ("google".equals(provider)) {
            return oAuth2User.getAttribute("email");
        } else if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        }
        return null;
    }

    private String getProviderFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI(); // 예: /oauth2/authorization/google
        if (uri.contains("google")) return "google";
        else if (uri.contains("kakao")) return "kakao";
        return null;
    }

}
