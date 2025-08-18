package com.potential_radar.PR.like.dto;

import com.potential_radar.PR.like.domain.TargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LikeRequestDto {

    @NotNull
    private Long targetId;

    @NotNull
    private TargetType targetType;
}
