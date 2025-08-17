package com.potential_radar.PR.user.dto;

import com.potential_radar.PR.user.model.ExperienceRange;
import com.potential_radar.PR.user.model.UserProfile;

import java.math.BigDecimal;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String techPartName,
        String profileImage,
        String bio,
        String bioShort,
        String phone,
        String githubUrl,
        String linkedinUrl,
        String websiteUrl,
        String region,
        boolean isPortfolioOpen,
        boolean isContactOpen,
        boolean isSearchOpen,
        BigDecimal reputationScore,
        int reviewCount,
        ExperienceRange experienceRange
) {
    public UserProfileResponse(UserProfile userProfile) {
        this(
                userProfile.getUserId(),
                userProfile.getUser().getNickname(),
                userProfile.getUser().getEmail(),
                userProfile.getTechPart().getName(),
                userProfile.getProfileImage(),
                userProfile.getBio(),
                userProfile.getBioShort(),
                userProfile.getPhone(),
                userProfile.getGithubUrl(),
                userProfile.getLinkedinUrl(),
                userProfile.getWebsiteUrl(),
                userProfile.getRegion(),
                userProfile.isPortfolioOpen(),
                userProfile.isContactOpen(),
                userProfile.isSearchOpen(),
                userProfile.getReputationScore(),
                userProfile.getReviewCount(),
                userProfile.getExperienceRange()
        );
    }
}