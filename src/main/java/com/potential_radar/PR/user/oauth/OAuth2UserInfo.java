package com.potential_radar.PR.user.oauth;

public interface OAuth2UserInfo {
    String getProvider();
    String getProviderId();
    String getEmail();
    String getName();

    // GoogleUserInfo, KakaoUserInfo 구현체들을 생성합니다. (response에서 파싱)
}
