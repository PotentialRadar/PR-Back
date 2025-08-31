package com.potential_radar.PR.user.dto.portfolios;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.user.domain.UserProfile;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 포트폴리오 목록 조회시 사용하는 요약 정보 DTO
 */
@Builder
public record PortfolioSummaryResponse(
        Long userId,
        String nickname,
        String techPartName,
        String profileImage,
        String bio,
        String jobTitle,
        BigDecimal reputationScore,
        int reviewCount,
        ExperienceRange experienceRange,
        int projectCount,
        int techStackCount,
        List<String> techStacks,
        long likeCount
) {
    public static PortfolioSummaryResponse from(UserProfile userProfile, int projectCount, int techStackCount, List<String> techStacks, long likeCount) {
        return PortfolioSummaryResponse.builder()
                .userId(userProfile.getUser().getUserId())
                .nickname(userProfile.getUser().getNickname())
                .techPartName(userProfile.getTechPart() != null ? userProfile.getTechPart().getName() : null)
                .profileImage(userProfile.getUser().getProfileImage())
                .bio(userProfile.getBio())
                .jobTitle(userProfile.getJobTitle())
                .reputationScore(userProfile.getReputationScore())
                .reviewCount(userProfile.getReviewCount())
                .experienceRange(userProfile.getExperienceRange())
                .projectCount(projectCount)
                .techStackCount(techStackCount)
                .techStacks(techStacks)
                .likeCount(likeCount)
                .build();
    }
}