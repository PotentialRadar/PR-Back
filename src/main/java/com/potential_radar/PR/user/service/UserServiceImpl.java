package com.potential_radar.PR.user.service;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.config.jwt.TokenProvider;
import com.potential_radar.PR.common.domain.TechPart;
import com.potential_radar.PR.user.dto.*;
import com.potential_radar.PR.user.domain.Provider;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserProfile;
import com.potential_radar.PR.user.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TechPartRepository techPartRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public User register(UserSignupRequest request) {
        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 1. User 엔티티 생성 (아직 DB에 저장되지 않음)
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .provider(Provider.EMAIL)
                .build();
        
        // 2. UserProfile 생성 및 User와 연결
        TechPart defaultTechPart = techPartRepository.findById(11L)  // Defualt : 11 ETC
                .orElseThrow(() -> new NotFoundException("기본 기술 분야를 찾을 수 없습니다"));
        user.initializeProfile(defaultTechPart); // User 엔티티의 헬퍼 메서드 사용

        // 3. User를 저장하면 UserProfile도 함께 저장됨 (Cascade)
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

    @Override
    public UserProfileResponse getUserProfile(String email) {
        User user = findByEmail(email);
        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("사용자 프로필을 찾을 수 없습니다"));
        return new UserProfileResponse(userProfile);
    }


    @Override
    public void updateUserProfile(String email, UserProfileUpdateRequest request) {
        User user = findByEmail(email);
        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("사용자 프로필을 찾을 수 없습니다"));
        
        // TechPart 유효성 검사
        if (request.techPartId() != null) {
            TechPart techPart = techPartRepository.findById(request.techPartId())
                    .orElseThrow(() -> new NotFoundException("기술 분야를 찾을 수 없습니다"));
            userProfile.setTechPart(techPart);
        }
        
        // 사용자 닉네임 업데이트 (프로필에서 닉네임도 변경 가능)
        if (request.nickname() != null && !user.getNickname().equals(request.nickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
            }
            userRepository.updateNickname(user.getUserId(), request.nickname());
        }
        
        // 프로필 정보 업데이트
        if (request.profileImage() != null) userProfile.setProfileImage(request.profileImage());
        if (request.bio() != null) userProfile.setBio(request.bio());
        if (request.phone() != null) userProfile.setPhone(request.phone());
        if (request.githubUrl() != null) userProfile.setGithubUrl(request.githubUrl());
        if (request.linkedinUrl() != null) userProfile.setLinkedinUrl(request.linkedinUrl());
        if (request.websiteUrl() != null) userProfile.setWebsiteUrl(request.websiteUrl());
        if (request.jobTitle() != null) {
            String jt = request.jobTitle().trim();
            userProfile.setJobTitle(jt.isEmpty() ? null : jt);
        }
        if (request.isPortfolioOpen() != null) userProfile.setPortfolioOpen(request.isPortfolioOpen());
        if (request.isContactOpen() != null) userProfile.setContactOpen(request.isContactOpen());
        if (request.isSearchOpen() != null) userProfile.setSearchOpen(request.isSearchOpen());
        if (request.experienceRange() != null) userProfile.setExperienceRange(request.experienceRange());
        
        userProfileRepository.save(userProfile);
    }

    @Override
    public void deleteUser(String email) {
        User user = findByEmail(email);
        
        // 프로필이 있다면 삭제
        userProfileRepository.findByUser(user).ifPresent(userProfileRepository::delete);
        
        // 리프레시 토큰 삭제
        refreshTokenService.deleteRefreshToken(user.getUserId());
        
        // 사용자 삭제
        userRepository.delete(user);
    }

}
