package com.potential_radar.PR.user.service;

import com.potential_radar.PR.project.domain.TeamMemberReview;
import com.potential_radar.PR.project.repository.TeamMemberReviewRepository;
import com.potential_radar.PR.user.dto.review.UserReceivedReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReviewService {

    private final TeamMemberReviewRepository teamMemberReviewRepository;

    public List<UserReceivedReviewResponse> getReceivedReviews(Long userId) {
        List<TeamMemberReview> reviews = teamMemberReviewRepository.findAllByRevieweeIdWithDetails(userId);
        
        return reviews.stream()
                .map(review -> UserReceivedReviewResponse.builder()
                        .reviewId(review.getId())
                        .projectId(review.getProject().getProjectId())
                        .projectTitle(review.getProject().getTitle())
                        .reviewerId(review.getReviewer().getUserId())
                        .reviewerNickname(review.getReviewer().getNickname())
                        .reviewerProfileImage(review.getReviewer().getProfileImage())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}