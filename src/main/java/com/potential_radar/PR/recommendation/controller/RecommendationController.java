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
     * 간편 피드백 - 추천그만받기
     */
    @PostMapping("/feedback/{recommendationHistoryId}/hide")
    public ResponseEntity<String> submitHide(
            @RequestHeader(value = "User-Id", required = false) Long userId,
            @PathVariable Long recommendationHistoryId
    ) {
        return submitFeedback(userId, recommendationHistoryId, FeedbackAction.HIDE);
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

    /**
     * 사용자가 현재 세션에서 이미 피드백을 제공했는지 확인 (세션 기반)
     * 프론트엔드에서 피드백 모달 표시 여부 결정에 사용
     */
    @GetMapping("/users/{userId}/should-show-feedback-modal")
    public ResponseEntity<Map<String, Object>> shouldShowFeedbackModal(
            @PathVariable Long userId,
            @RequestParam(required = false) String sessionId) {
        try {
            boolean shouldShow = recommendationService.shouldShowFeedbackModal(userId, sessionId);
            String message = shouldShow 
                ? "피드백 모달을 표시할 수 있습니다." 
                : (sessionId == null ? "세션 정보가 없습니다." : "이 세션에서 이미 피드백을 제공했습니다.");
                
            Map<String, Object> response = Map.of(
                "shouldShow", shouldShow,
                "message", message,
                "sessionId", sessionId != null ? sessionId : "none"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 피드백 모달 표시 여부 확인 실패: {}", e.getMessage());
            // 에러 시 기본적으로 모달을 표시하지 않음
            Map<String, Object> response = Map.of(
                "shouldShow", false,
                "message", "피드백 모달 표시 여부를 확인할 수 없습니다.",
                "sessionId", sessionId != null ? sessionId : "none"
            );
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 사용자가 숨김 처리한 프로젝트 ID 목록 조회 (AI 서버용)
     */
    @GetMapping("/users/{userId}/hidden-projects")
    public ResponseEntity<List<Long>> getHiddenProjects(@PathVariable Long userId) {
        try {
            List<Long> hiddenProjects = recommendationService.getHiddenProjectIds(userId);
            return ResponseEntity.ok(hiddenProjects);
        } catch (Exception e) {
            log.error("❌ 숨김 프로젝트 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(List.of()); // 빈 리스트 반환
        }
    }
}
