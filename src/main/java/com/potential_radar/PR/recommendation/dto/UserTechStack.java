package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 추천 시스템용 사용자 기술스택 DTO
 * 기술스택 이름과 사용자의 숙련도 레벨을 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTechStack {
    @JsonProperty("name")
    private String name;  // 기술스택 이름 (TechStack.name과 연동)

    @JsonProperty("level")
    private int level;    // 사용자 숙련도 (1-5)
}