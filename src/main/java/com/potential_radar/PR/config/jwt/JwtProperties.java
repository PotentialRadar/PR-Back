package com.potential_radar.PR.config.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Slf4j
@ConfigurationProperties(prefix = "jwt")
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

    private String issuer;
    private String secretKey;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;
}
