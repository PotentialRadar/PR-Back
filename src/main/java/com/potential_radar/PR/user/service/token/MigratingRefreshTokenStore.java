package com.potential_radar.PR.user.service.token;

import com.potential_radar.PR.user.service.token.TokenStoreProperties.ReadSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DB → Redis 전환을 담당하는 라우터. auth.token.* 설정에 따라 쓰기/읽기 경로를 고른다.
 *
 * - dual-write=true 동안은 DB가 원본(source of truth). Redis 쓰기 실패는 로그만 남긴다.
 * - read-source=REDIS 이고 Redis에 없으면 DB로 폴백하고 [TOKEN_FALLBACK] 로그를 남긴다.
 *   이 로그 건수(= fallbackCount)가 0으로 유지되면 3단계(dual-write=false)로 넘어간다.
 * - 재사용 탐지(used_token)는 Redis 읽기 경로에서만 동작한다.
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class MigratingRefreshTokenStore implements RefreshTokenStore {

    private final DbRefreshTokenStore db;
    private final RedisRefreshTokenStore redisStore;
    private final TokenStoreProperties props;

    private final AtomicLong fallbackCount = new AtomicLong();

    @Override
    public void save(Long userId, String token, Duration ttl) {
        if (props.isDualWrite()) {
            db.save(userId, token, ttl);
        }
        try {
            redisStore.save(userId, token, ttl);
        } catch (DataAccessException e) {
            if (!props.isDualWrite()) throw e;
            log.warn("[TOKEN_REDIS_WRITE_FAIL] save userId={}, DB remains source of truth", userId, e);
        }
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        if (props.getReadSource() == ReadSource.DB) {
            return db.findUserIdByToken(token);
        }

        Optional<Long> fromRedis;
        try {
            Optional<Long> reusedBy = redisStore.findOwnerOfUsedToken(token);
            if (reusedBy.isPresent()) {
                log.warn("[TOKEN_REUSE] 회전된 토큰 재사용 감지. userId={} 의 모든 토큰을 무효화합니다", reusedBy.get());
                deleteAllForUser(reusedBy.get());
                return Optional.empty();
            }
            fromRedis = redisStore.findUserIdByToken(token);
        } catch (DataAccessException e) {
            if (!props.isDualWrite()) throw e;
            log.warn("[TOKEN_REDIS_READ_FAIL] Redis 조회 실패, DB로 폴백합니다", e);
            return db.findUserIdByToken(token);
        }

        if (fromRedis.isPresent() || !props.isDualWrite()) {
            return fromRedis;
        }

        // 2단계: Redis에 없으면 DB로 폴백하고 불일치로 기록
        Optional<Long> fromDb = db.findUserIdByToken(token);
        fromDb.ifPresent(id -> {
            fallbackCount.incrementAndGet();
            log.info("[TOKEN_FALLBACK] token miss in redis, fallback to db. userId={}", id);
        });
        return fromDb;
    }

    /**
     * 재발급용 조회. findUserIdByToken 과 같지만, 회전된 토큰을 만나도 여기서 무효화하지 않고
     * 소유자만 돌려준다. 동시 요청(유예)인지 재사용 공격인지는 rotate()의 Lua 스크립트가 원자적으로 판정한다.
     */
    @Override
    public Optional<Long> findOwnerForRotation(String token) {
        if (props.getReadSource() == ReadSource.DB) {
            return db.findUserIdByToken(token);
        }

        Optional<Long> fromRedis;
        try {
            fromRedis = redisStore.findOwnerForRotation(token);
        } catch (DataAccessException e) {
            if (!props.isDualWrite()) throw e;
            log.warn("[TOKEN_REDIS_READ_FAIL] Redis 조회 실패, DB로 폴백합니다", e);
            return db.findUserIdByToken(token);
        }

        if (fromRedis.isPresent() || !props.isDualWrite()) {
            return fromRedis;
        }

        Optional<Long> fromDb = db.findUserIdByToken(token);
        fromDb.ifPresent(id -> {
            fallbackCount.incrementAndGet();
            log.info("[TOKEN_FALLBACK] token miss in redis, fallback to db. userId={}", id);
        });
        return fromDb;
    }

    @Override
    public RotateResult rotate(Long userId, String oldToken, String newToken, Duration ttl) {
        if (props.getReadSource() == ReadSource.DB) {
            db.rotate(userId, oldToken, newToken, ttl);
            try {
                redisStore.rotate(userId, oldToken, newToken, ttl);
            } catch (DataAccessException e) {
                log.warn("[TOKEN_REDIS_WRITE_FAIL] rotate userId={}, DB remains source of truth", userId, e);
            }
            return RotateResult.rotated(newToken);
        }

        RotateResult result;
        try {
            result = redisStore.rotate(userId, oldToken, newToken, ttl);
        } catch (DataAccessException e) {
            if (!props.isDualWrite()) throw e;
            log.warn("[TOKEN_REDIS_WRITE_FAIL] rotate userId={}, DB remains source of truth", userId, e);
            result = RotateResult.rotated(newToken);
        }
        // GRACE_HIT 은 먼저 온 요청이 이미 DB까지 반영하므로 여기서 다시 쓰지 않는다.
        if (result.outcome() == RotateResult.Outcome.ROTATED && props.isDualWrite()) {
            db.rotate(userId, oldToken, newToken, ttl);
        }
        return result;
    }

    @Override
    public void deleteAllForUser(Long userId) {
        if (props.isDualWrite()) {
            db.deleteAllForUser(userId);
        }
        try {
            redisStore.deleteAllForUser(userId);
        } catch (DataAccessException e) {
            if (!props.isDualWrite()) throw e;
            log.warn("[TOKEN_REDIS_WRITE_FAIL] delete userId={}", userId, e);
        }
    }

    /** 애플리케이션 기동 후 DB 폴백이 일어난 횟수 (정합성 확인용). */
    public long getFallbackCount() {
        return fallbackCount.get();
    }
}
