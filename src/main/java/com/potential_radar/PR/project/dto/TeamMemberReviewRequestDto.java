package com.potential_radar.PR.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberReviewRequestDto {
    @NotNull
    private Long projectId;
    @NotNull
    private Long revieweeId; // 리뷰 받을 사람의 ID
    @NotNull
    private Integer rating;
    private String comment;
}
