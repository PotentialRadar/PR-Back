package com.potential_radar.PR.config.oauth;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication; // ✅ 올바른 Authentication import
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

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
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            log.error("❌ OAuth2 로그인은 성공했지만 email이 없습니다.");
            response.sendRedirect("/api/login/fail");
            return;
        }

        // DB에서 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ 가입된 유저가 없습니다: {}", email);
                    return new IllegalArgumentException("가입된 유저가 없습니다.");
                });

        // JWT 생성
        String jwt = tokenProvider.generateToken(user, Duration.ofHours(2));

        // ✅ 방법 1: 프론트로 리다이렉트 시 토큰을 쿼리로 전달
        String redirectUrl = "http://localhost:3000/oauth2/callback?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);

        // ✅ 방법 2: SPA용 JSON 응답
//        response.setContentType("application/json;charset=UTF-8");
//        response.getWriter().write("{\"token\": \"" + jwt + "\"}");
//        response.getWriter().flush();

        log.info("✅ OAuth2 로그인 성공. JWT 발급 완료: {}", email);
    }
}
