package com.potential_radar.PR.config.oauth;

import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.model.UserProfile;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // 보통 로그인 기준값 (email 또는 username)
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // OAuth2 사용자라면 null 일 수 있음
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 추후 역할 기반 시스템이 도입되면 이 부분을 수정해야 합니다.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
