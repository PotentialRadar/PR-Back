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

    /**
     * 직전 Refresh Token 재사용 유예(초). 회전 직후 이 시간 안에 같은 옛 토큰이 다시 오고
     * 사용자의 현재 토큰이 그때 발급한 토큰 그대로면, 동시 요청으로 보고 같은 토큰을 돌려준다.
     * 0이면 유예 없음(옛 토큰은 즉시 재사용으로 판정). 길수록 오탐은 줄고 탈취 토큰이 끼어들 틈은 커진다.
     */
    private int reuseGraceSeconds = 3;

    @PostConstruct
    void validate() {
        if (readSource == ReadSource.DB && !dualWrite) {
            throw new IllegalStateException(
                    "auth.token.read-source=DB 는 auth.token.dual-write=true 일 때만 사용할 수 있습니다.");
        }
        if (reuseGraceSeconds < 0) {
            throw new IllegalStateException("auth.token.reuse-grace-seconds 는 0 이상이어야 합니다.");
        }
    }
}
