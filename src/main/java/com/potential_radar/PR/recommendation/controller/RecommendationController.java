package com.potential_radar.PR.recommendation.controller;

import com.potential_radar.PR.common.exception.RecommendationServiceException;
import com.potential_radar.PR.recommendation.domain.FeedbackAction;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 특정 사용자에게 적합한 프로젝트를 추천
     */
    @PostMapping("/projects")
    public ResponseEntity<List<RecommendedProjectResponse>> getRecommendedProjectsForUser(
            @RequestBody RecommendRequest request,
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(defaultValue = "0.5") double minScore,
            @RequestParam(defaultValue = "0.2") double minOverlap,
            @RequestParam(defaultValue = "false") boolean strict
    ) {
        List<RecommendedProjectResponse> result =
                recommendationService.getRecommendedProjectsForUser(request, topN, minScore, minOverlap, strict);
        
        log.info("✅ {}개 프로젝트 추천 완료 (사용자 ID: {})", result.size(), request.getUserId());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 추천에 대한 간단한 피드백 (좋아요/별로)
     */
    @PostMapping("/feedback/{recommendationHistoryId}")
    public ResponseEntity<String> submitFeedback(
            @RequestHeader(value = "User-Id", required = false) Long userId,
            @PathVariable Long recommendationHistoryId,
            @RequestParam FeedbackAction action // LIKE 또는 DISLIKE
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        try {
            recommendationService.saveFeedback(userId, recommendationHistoryId, action);
            String message = action == FeedbackAction.LIKE ? "좋아요를 표시했습니다." : "별로에요를 표시했습니다.";
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("❌ 피드백 저장 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body("피드백 처리에 실패했습니다.");
        }
    }

    /**
     * 간편 피드백 - 좋아요
     */
    @PostMapping("/feedback/{recommendationHistoryId}/like")
    public ResponseEntity<String> submitLike(
            @RequestHeader(value = "User-Id", required = false) Long userId,
            @PathVariable Long recommendationHistoryId
    ) {
        return submitFeedback(userId, recommendationHistoryId, FeedbackAction.LIKE);
    }

    /**
     * 간편 피드백 - 별로
     */
    @PostMapping("/feedback/{recommendationHistoryId}/dislike")
    public ResponseEntity<String> submitDislike(
            @RequestHeader(value = "User-Id", required = false) Long userId,
            @PathVariable Long recommendationHistoryId
    ) {
        return submitFeedback(userId, recommendationHistoryId, FeedbackAction.DISLIKE);
    }

    /**
     * 사용자 피드백 통계 조회 (AI 서버용)
     */
    @GetMapping("/users/{userId}/feedback-stats")
    public ResponseEntity<?> getUserFeedbackStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = recommendationService.getUserFeedbackStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ 사용자 피드백 통계 조회 실패: {}", e.getMessage());
            // AI 서버를 위해 기본값 반환
            Map<String, Object> defaultStats = Map.of(
                "totalFeedbacks", 0,
                "likeCount", 0,
                "dislikeCount", 0,
                "likeRatio", 0.5,
                "hasEnoughData", false
            );
            return ResponseEntity.ok(defaultStats);
        }
    }
}
