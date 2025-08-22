package com.potential_radar.PR.user.dto.experience;

import com.potential_radar.PR.user.domain.UserExperience;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserExperienceResponse(
        Long experienceId,
        String companyName,
        String department,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserExperienceResponse from(UserExperience experience) {
        return new UserExperienceResponse(
                experience.getExperienceId(),
                experience.getCompanyName(),
                experience.getDepartment(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.getIsCurrent(),
                experience.getSummary(),
                experience.getCreatedAt(),
                experience.getUpdatedAt()
        );
    }
}