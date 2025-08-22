package com.potential_radar.PR.user.dto.editInfo;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserProfile;

import java.math.BigDecimal;

public record UpdatedUserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String techPartName,
        String profileImage,
        String phone,
        String jobTitle,
        String githubUrl,
        String linkedinUrl,
        String websiteUrl,
        boolean isPortfolioOpen,
        boolean isContactOpen,
        boolean isSearchOpen,
        ExperienceRange experienceRange
) {
    public UpdatedUserProfileResponse(UserProfile userProfile, User user) {
        this(
                userProfile.getUser().getUserId(),
                userProfile.getUser().getNickname(),
                userProfile.getUser().getEmail(),
                userProfile.getTechPart() != null ? userProfile.getTechPart().getName() : null,
                user.getProfileImage(),
                userProfile.getPhone(),
                userProfile.getJobTitle(),
                userProfile.getGithubUrl(),
                userProfile.getLinkedinUrl(),
                userProfile.getWebsiteUrl(),
                userProfile.isPortfolioOpen(),
                userProfile.isContactOpen(),
                userProfile.isSearchOpen(),
                userProfile.getExperienceRange()
        );
    }
}