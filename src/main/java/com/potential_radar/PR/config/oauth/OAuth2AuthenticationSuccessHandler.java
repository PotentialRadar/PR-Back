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
 * 🎉 OAuth2 로그인 성공 핸들러 - 소셜 로그인의 마지막 관문!
 * 
 * 🤔 이 핸들러가 하는 일:
 * - Google, Kakao 등 OAuth2 소셜 로그인 성공 후 최종 처리
 * - 사용자 정보를 바탕으로 우리 서비스의 JWT 토큰 발급
 * - 토큰을 안전한 HttpOnly 쿠키로 설정하여 프론트엔드로 전달
 * - 프론트엔드 콜백 페이지로 자동 리다이렉트
 * 
 * 🔄 OAuth2 로그인 전체 흐름에서의 위치:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ **OAuth2 소셜 로그인 완전 흐름**                                  │
 * │                                                                 │
 * │ 1️⃣ 사용자: "Google로 로그인" 버튼 클릭                          │
 * │      ↓                                                          │
 * │ 2️⃣ 프론트엔드: /oauth2/authorization/google 요청               │
 * │      ↓                                                          │
 * │ 3️⃣ Spring Security: Google 인증 서버로 리다이렉트               │
 * │      ↓                                                          │
 * │ 4️⃣ Google: 사용자 인증 후 인증 코드와 함께 콜백                │
 * │      ↓                                                          │
 * │ 5️⃣ CustomOAuth2UserService: 사용자 정보 조회 및 DB 저장        │
 * │      ↓                                                          │
 * │ 6️⃣ **이 핸들러**: JWT 토큰 발급 및 쿠키 설정 ← 현재 위치!        │
 * │      ↓                                                          │
 * │ 7️⃣ 프론트엔드 콜백 페이지로 리다이렉트 (토큰은 쿠키로 전달)      │
 * │      ↓                                                          │
 * │ 8️⃣ 프론트엔드: 로그인 완료 처리                                │
 * └─────────────────────────────────────────────────────────────────┘
 * 
 * 🛡️ 보안 전략:
 * - **Redis 기반 토큰 관리**: 확장성과 성능을 위한 메모리 기반 저장
 * - **토큰 회전(Token Rotation)**: 매 갱신마다 새로운 토큰 쌍 생성
 * - **HttpOnly 쿠키**: JavaScript 접근 차단으로 XSS 공격 방지
 * - **토큰 재사용 탐지**: 의심스러운 토큰 활동 자동 감지 및 차단
 * - **예외 처리**: 실패 시 graceful한 에러 페이지 리다이렉트
 * 
 * 🎯 주요 처리 과정:
 * 1. OAuth2 인증 객체에서 사용자 이메일 추출
 * 2. 제공자별(Google/Kakao) 이메일 파싱 로직 적용
 * 3. DB에서 사용자 정보 조회 및 검증
 * 4. Redis 기반 JWT 토큰 쌍(Access + Refresh) 생성
 * 5. 토큰들을 HttpOnly 보안 쿠키로 설정
 * 6. 프론트엔드 성공 콜백 URL로 리다이렉트
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

    /**
     * 🎯 OAuth2 로그인 성공 시 호출되는 핵심 메소드
     * 
     * 이 메소드는 Spring Security OAuth2 체인의 마지막 단계에서 호출되어
     * 소셜 로그인 성공 후 우리 애플리케이션의 JWT 토큰을 발급하고
     * 사용자를 프론트엔드로 안전하게 리다이렉트합니다.
     * 
     * ┌─────────────────────────────────────────────────────────────────┐
     * │ 🔄 **OAuth2 성공 처리 흐름**                                     │
     * │                                                                 │
     * │ 1️⃣ Authentication 객체에서 OAuth2 사용자 정보 추출              │
     * │      ├─ Google: email 필드 직접 접근                            │
     * │      └─ Kakao: kakao_account.email 중첩 접근                    │
     * │      ↓                                                          │
     * │ 2️⃣ 추출된 이메일로 DB에서 사용자 조회                           │
     * │      ├─ ✅ 존재 → 토큰 발급 진행                                │
     * │      └─ ❌ 없음 → 에러 처리 (이론적으로 발생하지 않아야 함)       │
     * │      ↓                                                          │
     * │ 3️⃣ Redis 기반 JWT 토큰 쌍 생성                                 │
     * │      ├─ Access Token (30분, API 호출용)                        │
     * │      └─ Refresh Token (7일, 토큰 갱신용)                       │
     * │      ↓                                                          │
     * │ 4️⃣ HttpOnly 보안 쿠키로 토큰 설정                              │
     * │      ├─ access_token 쿠키 (XSS 방지)                           │
     * │      └─ refresh_token 쿠키 (보안 강화)                         │
     * │      ↓                                                          │
     * │ 5️⃣ 프론트엔드 콜백 URL로 리다이렉트                             │
     * │      └─ 토큰은 이미 쿠키로 설정되어 자동 전달                    │
     * └─────────────────────────────────────────────────────────────────┘
     * 
     * @param request HTTP 요청 객체 (OAuth2 제공자 정보 추출용)
     * @param response HTTP 응답 객체 (쿠키 설정 및 리다이렉트용)
     * @param authentication Spring Security 인증 객체 (OAuth2 사용자 정보 포함)
     * @throws IOException 리다이렉트 실패 시
     * @throws ServletException 서블릿 처리 오류 시
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 🔍 1단계: OAuth2 인증 객체에서 사용자 정보 추출
        Object principal = authentication.getPrincipal();

        // 🛡️ 타입 안전성 검사: DefaultOAuth2User인지 확인
        if (!(principal instanceof DefaultOAuth2User)) {
            log.error("❌ OAuth2User가 아닙니다: {}", principal.getClass().getName());
            response.sendRedirect("/api/login/fail");
            return;
        }

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;
        
        // 📧 2단계: 제공자별 이메일 추출 (Google vs Kakao 구조 차이 처리)
        String email = extractEmail(oAuth2User, getProviderFromRequest(request));

        if (email == null) {
            log.error("❌ OAuth2 로그인은 성공했지만 email이 없습니다.");
            response.sendRedirect("/api/login/fail");
            return;
        }

        try {
            // 👤 3단계: DB에서 사용자 조회 및 검증
            // 💡 CustomOAuth2UserService에서 이미 사용자를 생성/업데이트했어야 함
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("❌ OAuth2 로그인 후 유저를 찾을 수 없습니다: {}", email);
                        return new OAuth2AuthenticationException("OAuth2 로그인 처리 중 오류가 발생했습니다.");
                    });

            // 🎫 4단계: Redis 기반 JWT 토큰 쌍 생성
            // 💡 Access Token(30분) + Refresh Token(7일) 동시 생성
            // 💡 Refresh Token은 Redis에 저장되어 회전 및 재사용 탐지 지원
            TokenService.TokenPair tokenPair = tokenService.createTokenPair(user);

            // 🍪 5단계: Access Token을 HttpOnly 보안 쿠키로 설정
            // 🛡️ HttpOnly: JavaScript 접근 차단으로 XSS 공격 방지
            // 🛡️ Secure: HTTPS에서만 전송 (프로덕션 환경)
            // 🛡️ SameSite: CSRF 공격 완화
            int accessTokenMaxAge = (int) (jwtProperties.getAccessTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "access_token", tokenPair.getAccessToken(), accessTokenMaxAge);

            // 🍪 6단계: Refresh Token을 HttpOnly 보안 쿠키로 설정
            // 💡 더 긴 만료 시간으로 지속적인 로그인 유지
            int refreshTokenMaxAge = (int) (jwtProperties.getRefreshTokenExpiration() / 1000); // 밀리초 → 초
            CookieUtil.addCookie(response, "refresh_token", tokenPair.getRefreshToken(), refreshTokenMaxAge);

            // 📍 7단계: 프론트엔드 성공 콜백 페이지로 리다이렉트
            // 💡 토큰은 이미 쿠키로 설정되어 브라우저가 자동으로 포함해서 전송
            // 💡 프론트엔드에서는 별도 토큰 처리 없이 바로 인증된 상태로 시작
            response.sendRedirect(frontendCallbackUrl);

            log.info("✅ OAuth2 로그인 성공. Redis 토큰 발급 완료: {}", email);

        } catch (Exception e) {
            // 🚨 예외 발생 시 graceful한 에러 처리
            log.error("🔥 OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            response.sendRedirect("/api/login/fail");
        }
    }

    /**
     * 📧 OAuth2 제공자별 이메일 추출 전략
     * 
     * Google과 Kakao의 OAuth2 응답 구조가 다르기 때문에
     * 제공자별로 적합한 이메일 추출 로직을 적용합니다.
     * 
     * 🔍 제공자별 응답 구조:
     * ┌─────────────────────────────────────────────────────────────────┐
     * │ **Google OAuth2 응답 구조**                                      │
     * │ {                                                               │
     * │   "sub": "123456789",                                           │
     * │   "name": "홍길동",                                              │
     * │   "email": "user@gmail.com",           ← 직접 접근 가능          │
     * │   "picture": "https://...",                                     │
     * │   "email_verified": true                                        │
     * │ }                                                               │
     * └─────────────────────────────────────────────────────────────────┘
     * 
     * ┌─────────────────────────────────────────────────────────────────┐
     * │ **Kakao OAuth2 응답 구조**                                       │
     * │ {                                                               │
     * │   "id": 987654321,                                              │
     * │   "kakao_account": {                                            │
     * │     "email": "user@kakao.com",         ← 중첩 구조로 접근 필요   │
     * │     "email_verified": true                                      │
     * │   },                                                            │
     * │   "profile": { ... }                                            │
     * │ }                                                               │
     * └─────────────────────────────────────────────────────────────────┘
     * 
     * @param oAuth2User OAuth2 사용자 정보 객체
     * @param provider OAuth2 제공자 식별자 ("google" 또는 "kakao")
     * @return 추출된 이메일 주소, 실패 시 null
     */
    private String extractEmail(DefaultOAuth2User oAuth2User, String provider) {
        if ("google".equals(provider)) {
            // 🎯 Google: 최상위 레벨에서 직접 이메일 접근
            return oAuth2User.getAttribute("email");
        } else if ("kakao".equals(provider)) {
            // 🎯 Kakao: kakao_account 객체 내부의 email 필드 접근
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        }
        // 💡 지원하지 않는 제공자의 경우 null 반환
        return null;
    }

    /**
     * 🔍 HTTP 요청 URI에서 OAuth2 제공자 식별
     * 
     * Spring Security OAuth2가 생성하는 콜백 URI 패턴을 분석하여
     * 현재 로그인에 사용된 OAuth2 제공자를 식별합니다.
     * 
     * 📋 URI 패턴 예시:
     * - Google: /login/oauth2/code/google
     * - Kakao: /login/oauth2/code/kakao
     * - 인증 시작: /oauth2/authorization/google
     * 
     * @param request HTTP 요청 객체 (URI 정보 포함)
     * @return 식별된 제공자 이름 ("google", "kakao"), 알 수 없으면 null
     */
    private String getProviderFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI(); // 예: /oauth2/authorization/google
        if (uri.contains("google")) return "google";
        else if (uri.contains("kakao")) return "kakao";
        // 💡 새로운 OAuth2 제공자 추가 시 여기에 조건 추가
        return null;
    }

}
