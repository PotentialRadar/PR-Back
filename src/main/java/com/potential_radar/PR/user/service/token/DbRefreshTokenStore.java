package com.potential_radar.PR.user.service.token;

import com.potential_radar.PR.user.domain.RefreshToken;
import com.potential_radar.PR.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 기존 refresh_tokens 테이블(JPA)을 감싼 저장소. 사용자당 토큰 1개(user_id UNIQUE).
 */
@Component
@RequiredArgsConstructor
public class DbRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository repository;

    @Override
    @Transactional
    public void save(Long userId, String token, Duration ttl) {
        Instant expiry = Instant.now().plus(ttl);
        RefreshToken entity = repository.findByUserId(userId)
                .map(e -> e.update(token, expiry))
                .orElseGet(() -> new RefreshToken(userId, token, expiry));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findUserIdByToken(String token) {
        return repository.findByRefreshToken(token)
                .filter(e -> e.getExpiryDate().isAfter(Instant.now()))
                .map(RefreshToken::getUserId);
    }

    @Override
    @Transactional
    public RotateResult rotate(Long userId, String oldToken, String newToken, Duration ttl) {
        save(userId, newToken, ttl);
        return RotateResult.rotated(newToken);
    }

    @Override
    @Transactional
    public void deleteAllForUser(Long userId) {
        repository.deleteByUserId(userId);
    }
}
