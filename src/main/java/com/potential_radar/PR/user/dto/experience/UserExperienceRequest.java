package com.potential_radar.PR.user.dto.experience;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UserExperienceRequest(
        @NotBlank(message = "회사명은 필수입니다")
        String companyName,
        
        String department,
        
        @NotNull(message = "시작일은 필수입니다")
        LocalDate startDate,
        
        LocalDate endDate,
        
        Boolean isCurrent,
        
        String summary
) {
    public UserExperienceRequest {
        if (isCurrent == null) {
            isCurrent = false;
        }
    }
}