package com.potential_radar.PR.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Optional;

/**
 * 🍪 HTTP 쿠키 관리 유틸리티 클래스
 * 
 * 보안이 강화된 쿠키 생성, 읽기, 삭제를 담당하는 유틸리티입니다.
 * JWT 토큰을 HttpOnly + Secure 쿠키로 관리하여 XSS 공격을 방지합니다.
 * 
 * 보안 기능:
 * - HttpOnly: JavaScript로 쿠키 접근 차단 (XSS 방어)
 * - Secure: HTTPS에서만 쿠키 전송 (프로덕션 환경)
 * - SameSite: CSRF 공격 방어 (추후 설정 가능)
 * - 자동 만료 시간 설정으로 토큰 수명 관리
 * 
 * 사용 용도:
 * - Access Token 쿠키 저장/조회 (30분 만료)
 * - Refresh Token 쿠키 저장/조회 (14일 만료)
 * - OAuth2 인증 상태 임시 저장
 */
@Slf4j
public class CookieUtil {

    /**
     * 🏭 보안 쿠키 생성 메소드
     * 
     * JWT 토큰을 안전한 HttpOnly 쿠키로 저장합니다.
     * XSS 공격으로부터 토큰을 보호하고, 브라우저에서 자동으로 전송됩니다.
     * 
     * @param response HTTP 응답 객체
     * @param name 쿠키 이름 (예: "access_token", "refresh_token")
     * @param value 쿠키 값 (JWT 토큰 문자열)
     * @param maxAge 쿠키 유효 시간 (초 단위, -1이면 브라우저 세션 종료까지)
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");                    // 🌐 전체 사이트에서 쿠키 사용 가능
        cookie.setHttpOnly(true);              // 🛡️ JavaScript 접근 차단 (XSS 방어)
        cookie.setMaxAge(maxAge);              // ⏰ 쿠키 만료 시간 설정
        
        // 🔒 환경별 보안 설정 적용
        addSecurityAttributes(cookie);
        
        response.addCookie(cookie);
        log.info("🍪 쿠키 생성됨: {} (만료: {}초)", name, maxAge);
    }

    /**
     * 🔍 쿠키 조회 메소드
     * 
     * 요청에서 특정 이름의 쿠키 값을 찾아 반환합니다.
     * JWT 토큰 인증 시 Access Token 쿠키를 조회할 때 사용됩니다.
     * 
     * @param request HTTP 요청 객체
     * @param name 조회할 쿠키 이름
     * @return Optional<Cookie> - 쿠키가 존재하면 쿠키 객체, 없으면 빈 Optional
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    log.debug("🔍 쿠키 조회 성공: {}", name);
                    return Optional.of(cookie);
                }
            }
        }
        
        log.debug("❌ 쿠키 조회 실패: {}", name);
        return Optional.empty();
    }

    /**
     * 🗑️ 쿠키 삭제 메소드
     * 
     * 기존 쿠키를 만료시켜 브라우저에서 제거합니다.
     * 로그아웃 시 Access Token과 Refresh Token 쿠키를 삭제할 때 사용됩니다.
     * 
     * @param response HTTP 응답 객체
     * @param name 삭제할 쿠키 이름
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");                    // 🌐 동일한 경로 설정 필요
        cookie.setHttpOnly(true);              // 🛡️ 보안 설정 유지
        cookie.setMaxAge(0);                   // ⏰ 즉시 만료로 삭제
        
        // 🔒 삭제 시에도 동일한 보안 설정 적용
        addSecurityAttributes(cookie);
        
        response.addCookie(cookie);
        log.info("🗑️ 쿠키 삭제됨: {}", name);
    }

    /**
     * 🔄 객체 직렬화하여 쿠키에 저장 (OAuth2 상태 관리용)
     * 
     * 복잡한 객체를 Base64로 인코딩하여 쿠키에 저장합니다.
     * OAuth2 인증 과정에서 임시 상태 정보를 저장할 때 사용됩니다.
     * 
     * @param response HTTP 응답 객체
     * @param name 쿠키 이름
     * @param obj 저장할 객체 (Serializable이어야 함)
     * @param maxAge 쿠키 유효 시간 (초)
     */
    public static void addSerializedCookie(HttpServletResponse response, String name, Object obj, int maxAge) {
        String value = Base64.getUrlEncoder()
            .encodeToString(SerializationUtils.serialize(obj));
        addCookie(response, name, value, maxAge);
    }

    /**
     * 🔄 쿠키에서 객체 역직렬화 (OAuth2 상태 복원용)
     * 
     * Base64로 인코딩된 쿠키 값을 객체로 복원합니다.
     * OAuth2 인증 완료 후 임시 저장된 상태 정보를 복원할 때 사용됩니다.
     * 
     * @param request HTTP 요청 객체
     * @param name 쿠키 이름
     * @param cls 복원할 객체의 클래스 타입
     * @return Optional<T> - 성공 시 객체, 실패 시 빈 Optional
     */
    public static <T> Optional<T> getDeserializedCookie(HttpServletRequest request, String name, Class<T> cls) {
        return getCookie(request, name)
            .map(cookie -> {
                try {
                    byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
                    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decodedBytes))) {
                        return cls.cast(ois.readObject());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    log.error("🔥 쿠키 역직렬화 실패: {}", name, e);
                    return null;
                }
            });
    }

    /**
     * 🔒 쿠키에 환경별 보안 속성 적용
     * 
     * 개발환경과 프로덕션환경에 따라 적절한 보안 설정을 적용합니다.
     * 
     * 보안 속성:
     * - Secure: HTTPS에서만 쿠키 전송 (프로덕션에서만 true)
     * - SameSite=Lax: CSRF 공격 방어 (일반적인 사용성 고려)
     * 
     * @param cookie 보안 속성을 적용할 쿠키 객체
     */
    private static void addSecurityAttributes(Cookie cookie) {
        // 🌍 현재 환경 감지 (프로덕션 환경에서는 HTTPS 필수)
        boolean isProduction = isProductionEnvironment();
        
        if (isProduction) {
            // 🔒 프로덕션: HTTPS에서만 쿠키 전송
            cookie.setSecure(true);
            log.debug("🔒 Secure 플래그 활성화됨 (프로덕션 환경)");
        } else {
            // 🔧 개발: HTTP에서도 쿠키 전송 허용
            cookie.setSecure(false);
            log.debug("🔧 Secure 플래그 비활성화됨 (개발 환경)");
        }
        
        // 🚫 SameSite=Lax: 일반적인 CSRF 보호 (Strict보다 사용성 좋음)
        // Note: Jakarta Servlet 6.0+에서 직접 지원, 이전 버전은 setAttribute 사용
        try {
            cookie.setAttribute("SameSite", "Lax");
            log.debug("🚫 SameSite=Lax 설정 완료");
        } catch (Exception e) {
            // 이전 버전 호환성을 위한 fallback (필요시 확장)
            log.warn("⚠️ SameSite 속성 설정 실패 (구버전 서블릿): {}", e.getMessage());
        }
    }

    /**
     * 🌍 현재 실행 환경이 프로덕션인지 확인
     * 
     * Spring Profile이나 환경변수를 확인하여 프로덕션 환경 여부를 판단합니다.
     * 
     * 판단 기준:
     * 1. spring.profiles.active 프로파일 확인
     * 2. 환경변수 확인 (SPRING_PROFILES_ACTIVE)
     * 3. localhost 여부 확인 (fallback)
     * 
     * @return 프로덕션 환경이면 true, 개발환경이면 false
     */
    private static boolean isProductionEnvironment() {
        // 🔍 Spring Profile 확인 (JVM 속성)
        String activeProfiles = System.getProperty("spring.profiles.active");
        if (activeProfiles != null) {
            boolean isProduction = activeProfiles.toLowerCase().contains("prod");
            log.debug("🔍 Spring Profile 감지: {} -> Production: {}", activeProfiles, isProduction);
            return isProduction;
        }
        
        // 🌐 환경변수 확인
        activeProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (activeProfiles != null) {
            boolean isProduction = activeProfiles.toLowerCase().contains("prod");
            log.debug("🌐 환경변수 감지: {} -> Production: {}", activeProfiles, isProduction);
            return isProduction;
        }
        
        // 📡 기본값: localhost 기반 판단 (개발환경 가정)
        String serverPort = System.getProperty("server.port", "8080");
        boolean isLocalhost = "localhost".equals(System.getProperty("server.address", "localhost")) || 
                             "8080".equals(serverPort);
        
        log.debug("📡 기본 환경 감지 -> localhost: {} (Production: {})", isLocalhost, !isLocalhost);
        return !isLocalhost; // localhost가 아니면 프로덕션으로 가정
    }
}