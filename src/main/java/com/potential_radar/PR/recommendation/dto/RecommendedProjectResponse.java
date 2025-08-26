package com.potential_radar.PR.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProjectResponse {
    private Long recommendationHistoryId; // 피드백을 위한 추천 이력 ID
    private Long projectId;
    private String title;
    private String description;
    private Double matchScore;

    private List<String> projectTechStacks;
    private RecommendationExplanation explanation;
    
    // 프론트엔드에서 필요한 추가 필드들
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer recruitCount;
    private Integer appliedCount;
    private LocalDate recruitDeadline;
}
