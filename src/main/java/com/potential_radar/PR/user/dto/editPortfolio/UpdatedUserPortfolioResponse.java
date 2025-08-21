package com.potential_radar.PR.user.dto.editPortfolio;

import com.potential_radar.PR.user.dto.education.UserEducationResponse;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;
import com.potential_radar.PR.user.dto.techStack.UserTechStackResponse;

import java.util.List;

public record UpdatedUserPortfolioResponse(
        Long userId,
        String profileImage,
        String nickname,
        String techPart,
        String jobTitle,
        String bio,
        List<UserEducationResponse> educations,
        List<UserExperienceResponse> experiences,
        List<UserTechStackResponse> techStacks
        // TODO: 프로젝트 (휘가 한거 불러와야함)
) {
}
