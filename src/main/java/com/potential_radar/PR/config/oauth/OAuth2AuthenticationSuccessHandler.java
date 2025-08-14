package com.potential_radar.PR.config.oauth;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

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

        // DB에서 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ 가입된 유저가 없습니다: {}", email);
                    return new OAuth2AuthenticationException("가입된 유저가 없습니다.");                });

        // 로컬 로그인과 동일하게 Access Token과 Refresh Token을 모두 생성
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = refreshTokenService.createAndSaveRefreshToken(user);

        // Refresh Token은 보안을 위해 HttpOnly 쿠키로 전달
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // HTTPS 환경에서만 전송되도록 설정
        refreshTokenCookie.setPath("/"); // 모든 경로에서 쿠키 사용
        refreshTokenCookie.setMaxAge((int) (tokenProvider.getJwtProperties().getRefreshTokenExpiration() / 1000)); // 만료시간 설정 (초 단위)
        response.addCookie(refreshTokenCookie);

        // Access Token은 프론트엔드가 바로 사용할 수 있도록 리다이렉트 URL의 쿼리 파라미터로 전달
        String redirectWithToken = frontendCallbackUrl + "?accessToken=" + accessToken;
        response.sendRedirect(redirectWithToken);

        log.info("✅ OAuth2 로그인 성공. JWT 발급 완료: {}", email);
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
