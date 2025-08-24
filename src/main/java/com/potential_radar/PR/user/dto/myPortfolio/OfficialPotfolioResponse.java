package com.potential_radar.PR.user.dto.myPortfolio;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserProfile;
import com.potential_radar.PR.user.dto.education.UserEducationResponse;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;
import com.potential_radar.PR.user.dto.project.UserProjectResponse;
import com.potential_radar.PR.user.dto.techStack.UserTechStackResponse;

import java.math.BigDecimal;
import java.util.List;

public record OfficialPotfolioResponse(
        Long userId,
        String nickname,
        String email,
        String techPartName,
        String profileImage,
        String bio,
        String phone,
        String jobTitle,
        String githubUrl,
        String linkedinUrl,
        String websiteUrl,
        boolean isPortfolioOpen,
        boolean isContactOpen,
        boolean isSearchOpen,
        BigDecimal reputationScore,
        int reviewCount,
        ExperienceRange experienceRange,
        List<UserEducationResponse> educations,
        List<UserExperienceResponse> experiences,
        List<UserTechStackResponse> techStacks,
        List<UserProjectResponse> projects
) {
    public OfficialPotfolioResponse(UserProfile userProfile, User user,
                                   List<UserEducationResponse> educations,
                                   List<UserExperienceResponse> experiences,
                                   List<UserTechStackResponse> techStacks,
                                   List<UserProjectResponse> projects) {
        this(
                userProfile.getUser().getUserId(),
                userProfile.getUser().getNickname(),
                userProfile.getUser().getEmail(),
                userProfile.getTechPart() != null ? userProfile.getTechPart().getName() : null,
                userProfile.getUser().getProfileImage(),
                userProfile.getBio(),
                userProfile.getPhone(),
                userProfile.getJobTitle(),
                userProfile.getGithubUrl(),
                userProfile.getLinkedinUrl(),
                userProfile.getWebsiteUrl(),
                userProfile.isPortfolioOpen(),
                userProfile.isContactOpen(),
                userProfile.isSearchOpen(),
                userProfile.getReputationScore(),
                userProfile.getReviewCount(),
                userProfile.getExperienceRange(),
                educations,
                experiences,
                techStacks,
                projects
        );
    }
}