package com.potential_radar.PR.config.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 📄 JWT 설정 속성 관리 클래스
 * 
 * application.yml 파일의 jwt.* 속성들을 자동으로 매핑하여 관리하는 설정 클래스입니다.
 * 
 * 주요 설정 항목:
 * - jwt.issuer: JWT 토큰 발급자 정보
 * - jwt.secret-key: JWT 서명에 사용할 비밀 키 (256비트 이상 필요)
 * - jwt.access-token-expiration: Access Token 만료 시간 (밀리세컨드, 기본 1시간)
 * - jwt.refresh-token-expiration: Refresh Token 만료 시간 (밀리세컨드, 기본 14일)
 * 
 * 보안 검증:
 * - @PostConstruct에서 모든 필수 속성의 유효성 검증
 * - Refresh Token이 Access Token보다 긴 만료 시간을 가지는지 경고
 */
@Getter  // 모든 필드에 대한 getter 메소드 자동 생성
@Setter  // 모든 필드에 대한 setter 메소드 자동 생성
@Component  // 스프링 빈으로 등록
@Slf4j  // 로깅 기능 사용
@ConfigurationProperties(prefix = "jwt")  // application.yml의 jwt.* 속성들을 자동 매핑
public class JwtProperties {

    @PostConstruct
    public void validate() {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("jwt.issuer 속성을 설정해주세요.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("jwt.secret-key 속성을 설정해주세요.");
        }
        if (accessTokenExpiration == null || accessTokenExpiration <= 0) {
            throw new IllegalStateException("Access 토큰 만료 시간(jwt.access-token-expiration)은 양수여야 합니다.");
        }
        if (refreshTokenExpiration == null || refreshTokenExpiration <= 0) {
            throw new IllegalStateException("Refresh 토큰 만료 시간(jwt.refresh-token-expiration)은 양수여야 합니다.");
        }
        if (refreshTokenExpiration <= accessTokenExpiration) {
            log.warn("경고: Refresh 토큰의 만료 시간은 Access 토큰의 만료 시간보다 길게 설정하는 것이 좋습니다.");
        }
    }

    // 🏢 JWT 토큰 발급자 정보 ("PotentialRadar" 등)
    private String issuer;
    
    // 🔐 JWT 서명에 사용할 비밀 키 (256비트 이상 필요)
    private String secretKey;
    
    // ⏰ Access Token 만료 시간 (밀리세컨드 단위, 예: 3600000 = 1시간)
    private Long accessTokenExpiration;
    
    // 🔄 Refresh Token 만료 시간 (밀리세컨드 단위, 예: 1209600000 = 14일)
    private Long refreshTokenExpiration;
}
