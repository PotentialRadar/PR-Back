package com.potential_radar.PR.user.dto.education;

import com.potential_radar.PR.user.domain.UserEducation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserEducationResponse(
        Long educationId,
        String institution,
        String program,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserEducationResponse from(UserEducation education) {
        return new UserEducationResponse(
                education.getEducationId(),
                education.getInstitution(),
                education.getProgram(),
                education.getStartDate(),
                education.getEndDate(),
                education.getIsCurrent(),
                education.getCreatedAt(),
                education.getUpdatedAt()
        );
    }
}