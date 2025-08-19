package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProject {
    
    private Long projectId;
    
    private String title;
    
    private String description;
    
    private Double matchScore;
    
    private List<String> projectTechStacks;
    
    private String status; // "RECRUITING", "IN_PROGRESS", "COMPLETED"
    
    private String recruitDeadline;
    
    private String startDate;
    
    private String endDate;
    
    private Integer recruitCount;
    
    private Integer appliedCount;
    
    private Integer viewCount;
    
    private ProjectExplanation explanation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectExplanation {
        @JsonProperty("main_reason")
        private String mainReason;
        
        @JsonProperty("matched_skills")
        private List<String> matchedSkills;
        
        @JsonProperty("growth_opportunities")
        private List<String> growthOpportunities;
        
        @JsonProperty("simple_explanation")
        private String simpleExplanation;
        
        @JsonProperty("difficulty_level")
        private String difficultyLevel;
        
        @JsonProperty("learning_potential")
        private Double learningPotential;
    }
}