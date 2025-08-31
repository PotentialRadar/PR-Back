package com.potential_radar.PR.config;

import com.potential_radar.PR.config.jwt.TokenAuthenticationFilter;
import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.config.oauth.OAuth2AuthenticationSuccessHandler;
import com.potential_radar.PR.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 🔐 Spring Security 중앙 설정 클래스
 * 
 * 이 클래스는 애플리케이션의 보안 정책을 정의하고 구성합니다.
 * - JWT 토큰 기반 인증 설정
 * - OAuth2 소셜 로그인 설정 (Google, Kakao)
 * - CORS 정책 설정
 * - 접근 권한 정의
 * - 보안 필터 체인 구성
 */
@Configuration  // 스프링 설정 클래스임을 선언
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor  // final 필드들을 위한 생성자 자동 생성
public class WebSecurityConfig {

    // 🔧 의존성 주입받는 보안 관련 서비스들
    private final UserDetailsService userService;  // Spring Security 사용자 정보 로딩 서비스
    private final TokenProvider tokenProvider;  // JWT 토큰 생성/검증 서비스
    private final CustomOAuth2UserService customOAuth2UserService;  // OAuth2 사용자 정보 처리 서비스
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;  // OAuth2 로그인 성공 후 처리 핸들러

    // 🌐 프론트엔드 포트 정보 (CORS 설정에 사용)
    @Value("${ports.frontend}")
    private String frontendPort;

//    // 스프링 시큐리티 기능 비활성화
//    @Bean
//    public WebSecurityCustomizer configure() {
//        return (web)->web.ignoring()
//                .requestMatchers(new AntPathRequestMatcher("/static/**"));
//    }

    /**
     * 🔍 JWT 토큰 인증 필터 빈 등록
     * 
     * 매 요청마다 Authorization 헤더나 쿠키에서 JWT 토큰을 추출하고 검증하여
     * SecurityContext에 인증 정보를 설정하는 필터입니다.
     */
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    /**
     * 🛡️ Spring Security 필터 체인 구성
     * 
     * HTTP 보안 설정의 핵심 메소드로, 다음과 같은 보안 정책을 설정합니다:
     * 1. CSRF 비활성화 (JWT 사용으로 인해 불필요)
     * 2. CORS 설정
     * 3. URL별 접근 권한 설정
     * 4. OAuth2 로그인 설정
     * 5. JWT 필터 등록
     * 6. 로그아웃 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 🚫 CSRF 비활성화: JWT 토큰 사용 시 CSRF 공격에 대한 보안이 내장되어 있음
                .csrf(csrf -> csrf.disable())
                // 📴 세션을 생성/사용하지 않는 무상태 정책 (JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 🌐 CORS 설정: 프론트엔드와의 교차 출처 리소스 공유 허용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 🔐 URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // ✅ 인증 없이 접근 가능한 공개 엔드포인트들
                        .requestMatchers(
                                "/api/login",        // 로그인 엔드포인트
                                "/api/signup",       // 회원가입 엔드포인트
                                "/api/token",        // 토큰 갱신 엔드포인트
                                "/api/auth/status",  // 인증 상태 확인 엔드포인트
                                "/oauth2/**",        // OAuth2 관련 모든 엔드포인트
                                "/login/oauth2/**",  // OAuth2 로그인 콜백 엔드포인트
                                "/api/login/**",     // 로그인 관련 모든 엔드포인트
                                "/api/user/*/likes/projects", // 특정 사용자의 좋아요 목록 공개 조회
                                "/api/recommend/**", // AI 추천 시스템 엔드포인트
                                "/test/**",          // 테스트용 엔드포인트
                                "/api/search/**"     // 검색 엔드포인트 (공개 검색 허용)
                        )
                        .permitAll()  // 위 경로들은 인증 없이 접근 허용
                        .requestMatchers("/api/projects/**").permitAll()  // 프로젝트 관련 엔드포인트도 공개 접근 허용
                        .anyRequest().authenticated())  // 그 외 모든 요청은 인증 필요
                // 🔐 OAuth2 소셜 로그인 설정
                .oauth2Login(oauth -> oauth
                        // 👤 사용자 정보 엔드포인트 설정
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)  // 커스텀 OAuth2 사용자 서비스 등록
                        )
                        .successHandler(oAuth2SuccessHandler)  // OAuth2 로그인 성공 시 JWT 토큰 발급 처리
                        .failureUrl("/api/login/fail")        // OAuth2 로그인 실패 시 리다이렉트 URL
                )

                // 🔧 JWT 토큰 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                // 이를 통해 모든 요청에서 JWT 토큰을 먼저 검증하고 인증 정보를 설정
                .addFilterBefore(tokenAuthenticationFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                // 🚪 Spring Security 기본 로그아웃 비활성화 (커스텀 로그아웃 API 사용)
                .logout(logout -> logout.disable());
        return http.build();
    }

    /**
     * 🔑 인증 관리자(AuthenticationManager) 빈 등록
     * 
     * Spring Security에서 인증을 처리하는 핵심 컴포넌트입니다.
     * DaoAuthenticationProvider를 사용하여 데이터베이스 기반 인증을 구현합니다.
     * 
     * @param encoder BCrypt 패스워드 인코더
     * @return 구성된 AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(BCryptPasswordEncoder encoder) throws Exception {
        // 📊 DB 기반 인증 제공자 생성 및 설정
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);  // 사용자 정보 로딩 서비스 설정
        authProvider.setPasswordEncoder(encoder);         // 패스워드 인코더 설정

        // 🏭 인증 제공자를 관리하는 ProviderManager 반환
        return new ProviderManager(authProvider);
    }


    /**
     * 🔒 BCrypt 패스워드 인코더 빈 등록
     * 
     * 사용자 패스워드를 안전하게 해시화하기 위한 인코더입니다.
     * BCrypt는 솔트(salt)를 자동으로 생성하여 레인보우 테이블 공격을 방지합니다.
     */
    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 🌐 CORS(Cross-Origin Resource Sharing) 설정
     * 
     * 프론트엔드 애플리케이션에서 백엔드 API에 접근할 수 있도록
     * 교차 출처 리소스 공유 정책을 설정합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 🎯 허용할 오리진 패턴 설정 (프론트엔드 주소)
        configuration.setAllowedOriginPatterns(List.of("http://localhost:" + frontendPort));
        // 📝 허용할 HTTP 메소드들 설정
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 📋 허용할 요청 헤더들 설정 (모든 헤더 허용)
        configuration.setAllowedHeaders(List.of("*"));
        // 📤 응답에 노출할 헤더들 설정 (JWT 토큰과 쿠키를 위한 헤더들)
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        // 🍪 자격 증명(쿠키, Authorization 헤더 등) 포함 요청 허용
        configuration.setAllowCredentials(true);

        // 🗺️ URL 기반 CORS 설정 소스 생성 및 모든 경로에 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 CORS 설정 적용
        return source;
    }
}
