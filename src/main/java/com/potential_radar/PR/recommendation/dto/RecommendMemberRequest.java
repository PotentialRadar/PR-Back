package com.potential_radar.PR.recommendation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendMemberRequest {
    private Long projectId;
    private List<String> requiredSkills;
    private Integer teamSize = 4; // 기본값 4명
    private String experienceLevel = "any"; // 기본값
}