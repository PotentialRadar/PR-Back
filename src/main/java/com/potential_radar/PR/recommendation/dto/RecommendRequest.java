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
}
