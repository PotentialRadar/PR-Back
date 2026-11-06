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
     * oldToken을 newToken으로 회전한다.
     * @return 회전 성공 여부. false면 이미 사용된 토큰(동시 재사용)이다.
     */
    boolean rotate(Long userId, String oldToken, String newToken, Duration ttl);

    /** 사용자의 활성 Refresh Token을 모두 무효화한다. */
    void deleteAllForUser(Long userId);
}
