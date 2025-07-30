package com.potential_radar.PR.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
// [팀원 대상] 사용자에게 적합한 프로젝트 목록을 추천
public class RecommendedProjectResponse {
    private Long projectId;     // 추천된 프로젝트 ID
    private String title;       // 프로젝트 제목
    private String description; // 프로젝트 설명
    private double matchScore;  // 추천 점수
}
