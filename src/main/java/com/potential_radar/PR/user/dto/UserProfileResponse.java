package com.potential_radar.PR.user.dto;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.user.domain.UserProfile;

import java.math.BigDecimal;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String techPartName,
        String profileImage,
        String bio,
        String phone,
        String githubUrl,
        String linkedinUrl,
        String websiteUrl,
        boolean isPortfolioOpen,
        boolean isContactOpen,
        boolean isSearchOpen,
        BigDecimal reputationScore,
        int reviewCount,
        ExperienceRange experienceRange
) {
    public UserProfileResponse(UserProfile userProfile) {
        this(
                userProfile.getUser().getUserId(),
                userProfile.getUser().getNickname(),
                userProfile.getUser().getEmail(),
                userProfile.getTechPart().getName(),
                userProfile.getProfileImage(),
                userProfile.getBio(),
                userProfile.getPhone(),
                userProfile.getGithubUrl(),
                userProfile.getLinkedinUrl(),
                userProfile.getWebsiteUrl(),
                userProfile.isPortfolioOpen(),
                userProfile.isContactOpen(),
                userProfile.isSearchOpen(),
                userProfile.getReputationScore(),
                userProfile.getReviewCount(),
                userProfile.getExperienceRange()
        );
    }
}