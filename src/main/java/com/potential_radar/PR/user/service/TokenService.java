package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    public String createNewAccessToken(String refreshToken){
        // 토큰 유효성 검사에 실패하면 예외 발생
        if(!tokenProvider.validToken(refreshToken)){
            throw new IllegalArgumentException("Invalid Refresh Token");
        }

        Long userId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();
        User user = userService.findById(userId);

        return tokenProvider.generateToken(user, Duration.ofHours(2));
    }

    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);
    }
}
