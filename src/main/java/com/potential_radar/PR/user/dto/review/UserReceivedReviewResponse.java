package com.potential_radar.PR.user.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReceivedReviewResponse {
    private Long reviewId;
    private Long projectId;
    private String projectTitle;
    private Long reviewerId;
    private String reviewerNickname;
    private String reviewerProfileImage;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}