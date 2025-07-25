package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.oauth.Google2UserInfo;
import com.potential_radar.PR.user.oauth.Kakao2UserInfo;
import com.potential_radar.PR.user.oauth.OAuth2UserInfo;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // google, kakao
        OAuth2UserInfo userInfo = getOAuth2UserInfo(provider, oAuth2User.getAttributes());

        // 회원가입 또는 기존 유저 조회
        User user = saveOrUpdate(userInfo);

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
        throw new IllegalArgumentException("지원하지 않는 Provider");
    }

    private User saveOrUpdate(OAuth2UserInfo userInfo) {
        return userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .nickname(userInfo.getName()) // 초기 닉네임 설정
                        .provider(User.Provider.valueOf(userInfo.getProvider().toUpperCase()))
                        .providerUserId(userInfo.getProviderId())
                        .isPortfolioOpen(false) // 기본값 설정
                        .profileImage(null)     // 필요시 설정
                        .reputationScore(null)  // @PrePersist에서 0으로 초기화
                        .reviewCount(0)
                        .build()));
    }


}
