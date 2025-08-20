package com.potential_radar.PR.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendProjectRequest {
    
    private Long userId;
    
    private List<UserTechStack> techStacks;
    
    private String experienceLevel; // "beginner", "intermediate", "advanced"
    
    private List<String> preferredCategories; // ["frontend", "backend", "fullstack"]
    
    private Integer maxResults;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTechStack {
        private String name;
        private Integer level; // 1-5
    }
}