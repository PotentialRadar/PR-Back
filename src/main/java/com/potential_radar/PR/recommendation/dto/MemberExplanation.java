package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberExplanation {
    @JsonProperty("main_reason")
    private String mainReason;
    
    @JsonProperty("detailed_reasons")
    private List<String> detailedReasons;
    
    @JsonProperty("matched_skills")
    private List<String> matchedSkills;
    
    @JsonProperty("growth_opportunities")
    private List<String> growthOpportunities;
    
    @JsonProperty("simple_explanation")
    private String simpleExplanation;
    
    @JsonProperty("experience_match")
    private String experienceMatch;
}