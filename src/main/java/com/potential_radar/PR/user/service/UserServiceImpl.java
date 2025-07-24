package com.potential_radar.PR.user.service;

import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.dto.LoginResponse;
import com.potential_radar.PR.user.dto.UserLoginRequest;
import com.potential_radar.PR.user.dto.UserSignupRequest;
import com.potential_radar.PR.user.model.RefreshToken;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.RefreshTokenRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public User register(UserSignupRequest request) {
        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickname(request.nickname())
                .isPortfolioOpen(false) //초기 기본값
                .provider(User.Provider.LOCAL)
                .reputationScore(null)
                .reviewCount(0)
        .build();


        return userRepository.save(user);
    }

    @Override
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findById(Long userId){
        return userRepository.findById(userId).orElseThrow(()->new IllegalArgumentException("Unexpected User"));
    }

    @Override
    public LoginResponse login(UserLoginRequest request) {
         // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 비밀번호 확인
        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 토큰 발급
        String accessToken = tokenProvider.generateToken(user, Duration.ofMinutes(30));
        String refreshToken = tokenProvider.generateToken(user, Duration.ofDays(14));

        // 4. refreshToken 저장 또는 갱신
        refreshTokenRepository.findByUserId(user.getUserId())
                .ifPresentOrElse(
                        rt -> rt.update(refreshToken),
                        ()-> refreshTokenRepository.save(new RefreshToken(user.getUserId(), refreshToken))
                );

        // 5. 응답 반환
        return new LoginResponse(accessToken, refreshToken);
    }
}
