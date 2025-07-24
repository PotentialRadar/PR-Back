package com.potential_radar.PR.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.security.Provider;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    private String password; // 소셜로그인 null 가능

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Lob //대용량 데이터(텍스트/바이너리) 매핑
    private String profileImage;

    @Column(nullable = false)
    private boolean isPortfolioOpen;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    private String providerUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private BigDecimal reputationScore;

    @Column(nullable = false)
    private int reviewCount = 0;


    @PrePersist
    protected void onCreate() {
        this.createdAt=  this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder //	Builder 패턴으로 객체 생성 시 유연한 코드 제공
    public User(String email, String password, String name, String nickname,
                String profileImage, boolean isPortfolioOpen,
                Provider provider, String providerUserId,
                BigDecimal reputationScore, int reviewCount) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.isPortfolioOpen = isPortfolioOpen;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.reputationScore = reputationScore;
        this.reviewCount = reviewCount;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("user"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    // 계정 만료 여부 반환
    @Override
    public boolean isAccountNonExpired() {
        // 만료되었는지 확인하는 로직
        return true; // true->만료되지 않았음
    }


    // 계정 잠금 여부 반환
    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠금되었는지 확인하는 로직
        return true; // true -> 잠금되지 않았음
    }


    // 패스워드의 만료 여부 반환
    @Override
    public boolean isCredentialsNonExpired() {
        // 패스워드가 만료되었는지 확인하는 로직
         return  true; // true -> 만료되지 않았음
    }

    // 계정 사용 가능 여부 반환
    @Override
    public boolean isEnabled() {
        // 계정이 사용 가능한지 확인하는 로직
        return  true; // true -> 사용 가능
    }

    public enum Provider {
        LOCAL, KAKAO, GOOGLE
    }




}
