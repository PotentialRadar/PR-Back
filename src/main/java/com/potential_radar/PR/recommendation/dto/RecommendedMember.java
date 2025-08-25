package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedMember {
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("profileImage")
    private String profileImage;
    
    @JsonProperty("matchScore")
    private Double matchScore;
    
    @JsonProperty("userTechStacks")
    private List<UserTechStackSimple> userTechStacks;
    
    // AI 서버 응답용 간단한 기술스택 DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTechStackSimple {
        private String name;
        private Integer level;
    }
    
    @JsonProperty("explanation")
    private MemberExplanation explanation;
    
    @JsonProperty("experience")
    private String experience;
    
    @JsonProperty("portfolioCount")
    private Integer portfolioCount;
    
    @JsonProperty("completedProjects")
    private Integer completedProjects;
    
    @JsonProperty("averageRating")
    private Double averageRating;
    
    @JsonProperty("lastActiveDate")
    private String lastActiveDate;
    
    @JsonProperty("isAvailable")
    private Boolean isAvailable;
    
    @JsonProperty("currentProjectCount")
    private Integer currentProjectCount;
}