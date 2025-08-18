package com.potential_radar.PR.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    private Long userId; // users.user_id와 공유 PK

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_profile_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tech_part_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_profile_techpart"))
    private TechPart techPart;

    @Column(columnDefinition = "text")
    private String profileImage;

    @Column
    private String bio;          // 자기소개

    @Column
    private String phone;

    @Column
    private String githubUrl;

    @Column
    private String linkedinUrl;

    @Column
    private String websiteUrl;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isPortfolioOpen = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isContactOpen = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isSearchOpen = false;

    @Column(nullable = false, precision = 5, scale = 2)
    @ColumnDefault("0.0")
    private BigDecimal reputationScore = BigDecimal.ZERO;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int reviewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceRange experienceRange = ExperienceRange.FRESHER;

    @Builder
    public UserProfile(User user, TechPart techPart, String profileImage, String bio,
                       String phone, String githubUrl, String linkedinUrl, String websiteUrl,
                       String region, Boolean isPortfolioOpen, Boolean isContactOpen,
                       Boolean isSearchOpen, BigDecimal reputationScore, Integer reviewCount,
                       String bioShort, ExperienceRange experienceRange) {
        this.user = user;
        this.techPart = techPart;
        this.profileImage = profileImage;
        this.bio = bio;
        this.phone = phone;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.websiteUrl = websiteUrl;
        if (isPortfolioOpen != null) this.isPortfolioOpen = isPortfolioOpen;
        if (isContactOpen   != null) this.isContactOpen   = isContactOpen;
        if (isSearchOpen    != null) this.isSearchOpen    = isSearchOpen;
        if (reputationScore != null) this.reputationScore = reputationScore;
        if (reviewCount     != null) this.reviewCount     = reviewCount;
        if (experienceRange != null) this.experienceRange = experienceRange;
    }
}
