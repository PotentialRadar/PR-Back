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

    // 이 필드가 Python API 응답에 실제로 포함되어 있고
    // 그 값이 JSON 객체들의 리스트일 경우에 사용
    // 만약 단순 문자열 리스트라면 List<String>을 사용
    private List<Object> projectTechStacks; // JSON 객체를 받기 위해 Object 타입으로 설정
}
