package com.potential_radar.PR.user.service.token;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis 저장소. 3계층 키를 사용하며 값은 모두 String으로 저장한다
 * (Long/Integer 역직렬화 문제를 원천 차단).
 *
 *   refresh_token:{userId} -> token       (사용자의 현재 토큰)
 *   token_mapping:{token}  -> userId      (토큰 → 사용자 역조회)
 *   used_token:{token}     -> userId      (회전된 토큰, 재사용 탐지용)
 *   reuse_grace:{token}    -> newToken    (회전 직후 N초 동안만. 동시 요청에 같은 새 토큰을 돌려주기 위함)
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    static final String TOKEN_MAPPING_PREFIX = "token_mapping:";
    static final String USED_TOKEN_PREFIX = "used_token:";
    static final String REUSE_GRACE_PREFIX = "reuse_grace:";
    static final Duration USED_TOKEN_TTL = Duration.ofDays(7);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final RedisScript<List<String>> ROTATE_SCRIPT =
            (RedisScript) new DefaultRedisScript<>(readScript("redis/rotate_refresh_token.lua"), List.class);

    private final StringRedisTemplate redis;
    private final TokenStoreProperties props;

    @Override
    public void save(Long userId, String token, Duration ttl) {
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        String previous = redis.opsForValue().get(userKey);
        if (previous != null) {
            redis.delete(TOKEN_MAPPING_PREFIX + previous);
        }
        redis.opsForValue().set(userKey, token, ttl);
        redis.opsForValue().set(TOKEN_MAPPING_PREFIX + token, String.valueOf(userId), ttl);
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        return Optional.ofNullable(redis.opsForValue().get(TOKEN_MAPPING_PREFIX + token))
                .map(Long::valueOf);
    }

    /** 재발급용 조회: 활성 토큰이면 그 소유자, 이미 회전된 토큰이면 원래 소유자. 판정은 rotate()가 한다. */
    @Override
    public Optional<Long> findOwnerForRotation(String token) {
        return findUserIdByToken(token).or(() -> findOwnerOfUsedToken(token));
    }

    @Override
    public RotateResult rotate(Long userId, String oldToken, String newToken, Duration ttl) {
        List<String> result = redis.execute(ROTATE_SCRIPT,
                List.of(USED_TOKEN_PREFIX + oldToken,
                        TOKEN_MAPPING_PREFIX + oldToken,
                        TOKEN_MAPPING_PREFIX + newToken,
                        REFRESH_TOKEN_PREFIX + userId,
                        REUSE_GRACE_PREFIX + oldToken),
                String.valueOf(userId),
                newToken,
                String.valueOf(ttl.toSeconds()),
                String.valueOf(USED_TOKEN_TTL.toSeconds()),
                String.valueOf(props.getReuseGraceSeconds()));
        return switch (result.get(0)) {
            case "ROTATED" -> RotateResult.rotated(result.get(1));
            case "GRACE_HIT" -> RotateResult.graceHit(result.get(1));
            default -> RotateResult.reuseDetected();
        };
    }

    @Override
    public void deleteAllForUser(Long userId) {
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        String current = redis.opsForValue().get(userKey);
        if (current != null) {
            redis.delete(TOKEN_MAPPING_PREFIX + current);
            markUsed(current, userId);
        }
        redis.delete(userKey);
    }

    /** 회전되어 더 이상 쓰면 안 되는 토큰이면 원래 소유자 ID를 반환한다. */
    public Optional<Long> findOwnerOfUsedToken(String token) {
        return Optional.ofNullable(redis.opsForValue().get(USED_TOKEN_PREFIX + token))
                .map(Long::valueOf);
    }

    private void markUsed(String token, Long userId) {
        redis.opsForValue().set(USED_TOKEN_PREFIX + token, String.valueOf(userId), USED_TOKEN_TTL);
    }

    private static String readScript(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Lua 스크립트를 읽을 수 없습니다: " + path, e);
        }
    }
}
