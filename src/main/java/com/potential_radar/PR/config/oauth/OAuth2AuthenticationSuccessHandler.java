package com.potential_radar.PR.config.oauth;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; // ✅ 올바른 Authentication import
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

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

        // JWT 생성
        String jwt = tokenProvider.generateToken(user, Duration.ofHours(2));

        // ✅ 방법 1: 프론트로 리다이렉트 시 토큰을 쿼리로 전달
        // 토큰을 HttpOnly 쿠키로 설정
        Cookie tokenCookie = new Cookie("auth-token", jwt);
                tokenCookie.setHttpOnly(true);
                tokenCookie.setSecure(false); // HTTPS에서만 사용  ✅ 개발 중에는 false
                tokenCookie.setPath("/");
                tokenCookie.setMaxAge(7200); // 2시간
                response.addCookie(tokenCookie);

        // 설정에서 가져온 안전한 리다이렉트 URL 사용
        String redirectWithToken = frontendCallbackUrl + "?token=" + jwt;
        response.sendRedirect(redirectWithToken);

        // ✅ 방법 2: SPA용 JSON 응답
//        response.setContentType("application/json;charset=UTF-8");
//        response.getWriter().write("{\"token\": \"" + jwt + "\"}");
//        response.getWriter().flush();

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
