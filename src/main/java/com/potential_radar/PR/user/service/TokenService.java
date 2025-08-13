package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.model.RefreshToken;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String createNewAccessToken(String refreshToken){
        // DB에서 리프레시 토큰을 찾아 유효성 검증
        RefreshToken foundRefreshToken = refreshTokenService.findByRefreshToken(refreshToken);

        // 리프레시 토큰이 만료되었는지 확인
        if (foundRefreshToken.isExpired()) {
            refreshTokenRepository.delete(foundRefreshToken); // 만료된 토큰은 삭제
            throw new IllegalArgumentException("Expired refresh token, please log in again.");
        }

        Long userId = foundRefreshToken.getUserId();
        User user = userService.findById(userId);

        return tokenProvider.generateAccessToken(user);
    }

    @Transactional
    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);
    }
}
