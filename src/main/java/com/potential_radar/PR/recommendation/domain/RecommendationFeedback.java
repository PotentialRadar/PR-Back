package com.potential_radar.PR.recommendation.domain;

import com.potential_radar.PR.common.domain.BaseTimeEntity;
import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 피드백 엔티티 - 간단한 좋아요/별로 피드백
 */
@Entity
@Table(name = "recommendation_feedback",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recommendation_history_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 피드백을 제공한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_history_id", nullable = false)
    private RecommendationHistory recommendationHistory; // 연관된 추천 이력

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_action", nullable = false)
    private FeedbackAction feedbackAction; // LIKE 또는 DISLIKE

    @Builder
    public RecommendationFeedback(User user, RecommendationHistory recommendationHistory, 
                                  FeedbackAction feedbackAction) {
        this.user = user;
        this.recommendationHistory = recommendationHistory;
        this.feedbackAction = feedbackAction;
    }

    /**
     * 피드백 점수 (단순화)
     * @return 1.0(좋아요) 또는 -1.0(별로)
     */
    public double getFeedbackScore() {
        return feedbackAction.isPositive() ? 1.0 : -1.0;
    }
}