package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.JwtProperties;
import com.potential_radar.PR.user.domain.RefreshToken;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken findByRefreshToken(String refreshToken){
        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(()->new IllegalArgumentException("Unexpected Token"));
    }

    @Transactional
    public String createAndSaveRefreshToken(User user) {
        // 리프레시 토큰은 JWT 형식이 아니어도 되므로, 간단한 UUID를 사용하거나 필요시 JWT로 생성할 수 있습니다.
        // 여기서는 간단한 UUID를 사용합니다.
        String tokenValue = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration());

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getUserId())
                .map(entity -> entity.update(tokenValue, expiryDate))
                .orElse(new RefreshToken(user.getUserId(), tokenValue, expiryDate));

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    @Transactional
    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
