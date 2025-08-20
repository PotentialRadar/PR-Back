package com.potential_radar.PR.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProjectResponse {
    private Long projectId;
    private String title;
    private String description;
    private Double matchScore;

    private List<String> projectTechStacks;
    private RecommendationExplanation explanation;
}
