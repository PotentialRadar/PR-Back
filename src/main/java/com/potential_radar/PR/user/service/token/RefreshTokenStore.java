package com.potential_radar.PR.user.service.token;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token 저장소 추상화. DB / Redis / 전환용 라우터가 같은 계약을 따른다.
 */
public interface RefreshTokenStore {

    /** 사용자의 Refresh Token을 저장한다. 기존 토큰이 있으면 대체한다. */
    void save(Long userId, String token, Duration ttl);

    /** 토큰으로 사용자 ID를 찾는다. 없거나 만료되었으면 empty. */
    Optional<Long> findUserIdByToken(String token);

    /**
     * 재발급용 조회. 이미 회전된 토큰이어도 소유자를 돌려준다.
     * 재사용 공격인지 동시 요청인지는 여기서 판단하지 않고 {@link #rotate}가 원자적으로 판정한다.
     */
    default Optional<Long> findOwnerForRotation(String token) {
        return findUserIdByToken(token);
    }

    /**
     * oldToken을 newToken으로 회전한다.
     * 유예 구간 안의 동시 요청이면 newToken 대신 직전에 발급된 토큰을 돌려준다(GRACE_HIT).
     */
    RotateResult rotate(Long userId, String oldToken, String newToken, Duration ttl);

    /** 사용자의 활성 Refresh Token을 모두 무효화한다. */
    void deleteAllForUser(Long userId);
}
