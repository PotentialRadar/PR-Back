package com.potential_radar.PR.like.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeStatusResponseDto {
    @JsonProperty("isLiked")
    private boolean isLiked;
}
