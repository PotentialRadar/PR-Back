package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.common.domain.TechPart;
import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.user.domain.Provider;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.oauth.Google2UserInfo;
import com.potential_radar.PR.user.oauth.Kakao2UserInfo;
import com.potential_radar.PR.user.oauth.OAuth2UserInfo;
import com.potential_radar.PR.common.repository.TechPartRepository;
import com.potential_radar.PR.user.repository.UserProfileRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TechPartRepository techPartRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // google, kakao
        OAuth2UserInfo userInfo = getOAuth2UserInfo(provider, oAuth2User.getAttributes());

        saveOrUpdate(userInfo);

        // Security 인증 객체 반환
        return new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                oAuth2User.getAttributes(),
                userRequest.getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName()
        );
    }

    private OAuth2UserInfo getOAuth2UserInfo(String provider , Map<String, Object> attributes){
        if("google".equals(provider)) return new Google2UserInfo(attributes);
        if("kakao".equals(provider)) return new Kakao2UserInfo(attributes);
        throw new OAuth2AuthenticationException("지원하지 않는 Provider: " + provider);
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo) {
        return userRepository.findByEmail(userInfo.getEmail())
                .map(existingUser -> {
                    // 기존 사용자는 업데이트 없이 그대로 반환
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 1. User 엔티티 생성 (아직 DB에 저장되지 않음)
                    User newUser = User.builder()
                            .email(userInfo.getEmail())
                            .nickname(generateRandomNickname())
                            .provider(Provider.valueOf(userInfo.getProvider().toUpperCase()))
                            .providerUserId(userInfo.getProviderId())
                            .build();

                    // 2. UserProfile 생성 및 User와 연결
                    TechPart defaultTechPart = techPartRepository.findById(11L)  // * Default : 11 Etc
                            .orElseThrow(() -> new NotFoundException("기본 기술 분야를 찾을 수 없습니다"));
                    newUser.initializeProfile(defaultTechPart); // User 엔티티의 헬퍼 메서드 사용

                    // 3. User를 저장하면 UserProfile도 함께 저장됨 (Cascade)
                    return userRepository.save(newUser);
                });
    }

    private String generateRandomNickname() {
        return "User" + UUID.randomUUID().toString().substring(0, 8);
    }


}
