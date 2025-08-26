package com.potential_radar.PR.user.dto.project;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PortfolioProjectSelectionRequest(
        @NotNull(message = "선택된 프로젝트 ID 목록은 필수입니다")
        List<Long> selectedProjectIds
) {
}