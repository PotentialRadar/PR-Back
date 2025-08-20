package com.potential_radar.PR.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeResponseDto {
    private long likeCount;
    private boolean isLiked;
}
