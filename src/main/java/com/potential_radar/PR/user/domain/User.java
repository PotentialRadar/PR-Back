package com.potential_radar.PR.user.domain;


import com.potential_radar.PR.common.domain.BaseTimeEntity;
import com.potential_radar.PR.tech.domain.TechPart;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = {"nickname"}),
                @UniqueConstraint(name = "uk_users_provider_providerUserId", columnNames = {"provider","provider_user_id"})
        }
)
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // citext 매핑: columnDefinition 으로 지정 (Hibernate는 String으로 처리 가능)
    // TODO : citext 필요 없지 않나?
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column
    private String password; // 소셜 로그인은 null 가능

    @Column(nullable = false, columnDefinition = "citext")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // EMAIL, GOOGLE, KAKAO

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(columnDefinition = "text")
    private String profileImage;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserProfile userProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserEducation> educations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserTechStack> userTechStacks;

    // 편의 메서드
    public void addEducation(UserEducation education) {
        educations.add(education);
        education.setUser(this);
    }

    public void removeEducation(UserEducation education) {
        educations.remove(education);
        education.setUser(null);
    }

    public void addExperience(UserExperience experience) {
        experiences.add(experience);
        experience.setUser(this);
    }

    public void removeExperience(UserExperience experience) {
        experiences.remove(experience);
        experience.setUser(null);
    }


    @Builder
    public User(String email, String password, String nickname,
                Provider provider, String providerUserId, String profileImage) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.profileImage = profileImage;
    }

    //== 연관관계 편의 메서드 ==//
    public void initializeProfile(TechPart defaultTechPart) {
        if (this.userProfile == null) {
            this.userProfile = UserProfile.builder()
                    .user(this)
                    .techPart(defaultTechPart)
                    .build();
        }
    }

    //== UserProfile 위임 메서드 ==//
    public TechPart getTechPart() {
        return this.userProfile != null ? this.userProfile.getTechPart() : null;
    }
    
    public ExperienceRange getExperienceRange() {
        return this.userProfile != null ? this.userProfile.getExperienceRange() : ExperienceRange.FRESHER;
    }
    
    public boolean isPortfolioOpen() {
        return this.userProfile != null && this.userProfile.isPortfolioOpen();
    }
    
    public boolean isSearchOpen() {
        return this.userProfile != null && this.userProfile.isSearchOpen();
    }
}
