package com.potential_radar.PR.recommendation.repository;

import com.potential_radar.PR.recommendation.domain.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

    // 특정 사용자의 피드백 조회
    List<RecommendationFeedback> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 추천 이력에 대한 피드백 조회
    List<RecommendationFeedback> findByRecommendationHistoryId(Long recommendationHistoryId);

    // 사용자가 특정 추천에 대해 이미 피드백했는지 확인
    boolean existsByUserUserIdAndRecommendationHistoryId(Long userId, Long recommendationHistoryId);

    // 사용자의 특정 추천에 대한 피드백 조회
    Optional<RecommendationFeedback> findByUserUserIdAndRecommendationHistoryId(Long userId, Long recommendationHistoryId);

    // 액션별 통계 (LIKE/DISLIKE)
    @Query("SELECT rf.feedbackAction, COUNT(rf) FROM RecommendationFeedback rf GROUP BY rf.feedbackAction")
    List<Object[]> getFeedbackStatsByAction();

    // 사용자의 좋아요 피드백 수
    @Query("SELECT COUNT(rf) FROM RecommendationFeedback rf WHERE rf.user.userId = :userId AND rf.feedbackAction = 'LIKE'")
    long countLikeFeedbackByUser(@Param("userId") Long userId);

    // 사용자의 총 피드백 수
    long countByUserUserId(Long userId);
}