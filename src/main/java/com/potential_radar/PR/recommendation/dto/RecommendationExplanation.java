package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationExplanation {
    @JsonProperty("main_reason")
    private String mainReason;
    
    @JsonProperty("detailed_reasons")
    private List<String> detailedReasons;
    
    @JsonProperty("score_breakdown")
    private Map<String, Double> scoreBreakdown;
    
    @JsonProperty("matched_skills")
    private List<String> matchedSkills;
    
    @JsonProperty("growth_opportunities")
    private List<String> growthOpportunities;
    
    @JsonProperty("simple_explanation")
    private String simpleExplanation;
}