package com.potential_radar.PR.user.dto.education;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UserEducationRequest(
        @NotBlank(message = "기관명은 필수입니다")
        String institution,
        
        String program,
        
        @NotNull(message = "시작일은 필수입니다")
        LocalDate startDate,
        
        LocalDate endDate,
        
        Boolean isCurrent
) {
    public UserEducationRequest {
        if (isCurrent == null) {
            isCurrent = false;
        }
    }
}