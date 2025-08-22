package com.potential_radar.PR.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest {
    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("techStacks")
    private List<UserTechStack> techStacks;
    
    @JsonProperty("likedProjects")
    private List<LikedProject> likedProjects;
    
    @JsonProperty("includeLikes")
    private Boolean includeLikes = true; // 좋아요 데이터 포함 여부
    
    public boolean isIncludeLikes() {
        return includeLikes != null ? includeLikes : true;
    }
}
