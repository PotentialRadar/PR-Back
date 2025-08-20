package com.potential_radar.PR.user.dto.editPortfolio;

import com.potential_radar.PR.user.dto.education.UserEducationRequest;
import com.potential_radar.PR.user.dto.experience.UserExperienceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserPortfolioUpdateRequest(
        @Size(max = 500, message = "자기소개는 500자 이하여야 합니다")
        String bio,
        
        @Valid
        List<UserEducationRequest> educations,
        
        @Valid
        List<UserExperienceRequest> experiences
        
        // TODO: 기술스택 리스트
        
) {
}
