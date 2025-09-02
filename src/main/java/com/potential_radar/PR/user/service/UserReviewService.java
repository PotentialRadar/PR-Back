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
                .map(review -> {
                    String profileImage = review.getReviewer().getProfileImage();
                    // 프로필 이미지가 null이거나 비어있으면 기본 아바타 URL 설정
                    if (profileImage == null || profileImage.trim().isEmpty()) {
                        profileImage = "https://api.dicebear.com/7.x/avataaars/svg?seed=" + review.getReviewer().getUserId();
                    }
                    
                    return UserReceivedReviewResponse.builder()
                            .reviewId(review.getId())
                            .projectId(review.getProject().getProjectId())
                            .projectTitle(review.getProject().getTitle())
                            .reviewerId(review.getReviewer().getUserId())
                            .reviewerNickname(review.getReviewer().getNickname())
                            .reviewerProfileImage(profileImage)
                            .rating(review.getRating())
                            .comment(review.getComment())
                            .createdAt(review.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}