package com.potential_radar.PR.user.dto.editPortfolio;

import jakarta.validation.constraints.Size;

public record UserPortfolioUpdateRequest(
        @Size(max = 500, message = "자기소개는 500자 이하여야 합니다")
        String bio

        // 교육이력 (CRUD)

        // 경력(CRUD),

        // 기술스택

        // 프로젝트

) {

}
