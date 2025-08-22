package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자가 좋아요한 프로젝트 정보 DTO
 * AI 추천 시스템에서 사용자의 취향 분석용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikedProject {
    @JsonProperty("projectId")
    private Long projectId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("techStacks")
    private List<String> techStacks;
    
    @JsonProperty("likedAt")
    private LocalDateTime likedAt;
    
    @JsonProperty("category") 
    private String category; // 프로젝트 카테고리 (웹, 모바일, AI 등)
}