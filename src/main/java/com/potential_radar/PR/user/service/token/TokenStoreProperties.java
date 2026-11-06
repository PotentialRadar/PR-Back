package com.potential_radar.PR.user.service.token;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 저장소 전환 설정 (auth.token.*)
 *
 * 단계  | dual-write | read-source | 설명
 * 1    | true       | DB          | DB + Redis 이중 쓰기, 읽기는 DB
 * 2    | true       | REDIS       | 읽기만 Redis로 이동, Redis에 없으면 DB 폴백
 * 3    | false      | REDIS       | Redis 단독 (DB 쓰기 제거)
 *
 * 되돌리기: read-source 를 DB 로 바꾸면 2단계 → 1단계로 즉시 복귀.
 * 설정이 없으면 3단계(Redis 단독, 기존 동작)로 동작한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.token")
public class TokenStoreProperties {

    public enum ReadSource { DB, REDIS }

    private boolean dualWrite = false;
    private ReadSource readSource = ReadSource.REDIS;

    @PostConstruct
    void validate() {
        if (readSource == ReadSource.DB && !dualWrite) {
            throw new IllegalStateException(
                    "auth.token.read-source=DB 는 auth.token.dual-write=true 일 때만 사용할 수 있습니다.");
        }
    }
}
