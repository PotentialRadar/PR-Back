package com.potential_radar.PR.user.service;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.user.dto.LoginResponse;
import com.potential_radar.PR.user.dto.UserLoginRequest;
import com.potential_radar.PR.user.dto.UserSignupRequest;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

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
        return userRepository.findById(userId).orElseThrow(()->new NotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(()->new NotFoundException("사용자를 찾을 수 없습니다. 이메일: " + email));
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

        // 3. 액세스 토큰 생성 (만료 시간은 JwtProperties에서 관리)
        String accessToken = tokenProvider.generateAccessToken(user);
        // 4. 리프레시 토큰 생성 및 저장 (UUID 기반, 로직은 RefreshTokenService에 위임)
        String refreshToken = refreshTokenService.createAndSaveRefreshToken(user);

        // 5. 응답 반환
        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public boolean existsbynickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }


}
