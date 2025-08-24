package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 추천 요청 DTO
 * 사용자 ID만 받아서 DB에서 사용자 기술스택을 자동 조회
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest {
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("techStacks")
    private List<TechStackForAI> techStacks; // AI 서버로 보낼 기술스택 객체 리스트
    
    @JsonProperty("likedProjects")
    private List<LikedProject> likedProjects;
    
    @JsonProperty("includeLikes")
    private Boolean includeLikes = true; // 좋아요 데이터 포함 여부
    
    public boolean isIncludeLikes() {
        return includeLikes != null ? includeLikes : true;
    }
    
    /**
     * AI 서버로 보낼 기술스택 정보 (Python schemas와 일치)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechStackForAI {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("level")
        private Integer level;
    }
}
