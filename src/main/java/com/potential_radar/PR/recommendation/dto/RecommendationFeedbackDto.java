package com.potential_radar.PR.recommendation.dto;

import com.potential_radar.PR.recommendation.domain.FeedbackAction;
import com.potential_radar.PR.recommendation.domain.FeedbackType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 추천 피드백 관련 DTO
 */
public class RecommendationFeedbackDto {

    /**
     * 피드백 제출 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class SubmitRequest {
        private Long recommendationHistoryId; // 추천 이력 ID
        private FeedbackAction feedbackAction; // 피드백 액션
        private Integer rating; // 1-5점 평점 (선택적)
        private String reason; // 피드백 이유 (선택적)
        private String comment; // 사용자 의견 (선택적)
        private boolean isAnonymous = false; // 익명 여부

        @Builder
        public SubmitRequest(Long recommendationHistoryId, FeedbackAction feedbackAction,
                            Integer rating, String reason, String comment, boolean isAnonymous) {
            this.recommendationHistoryId = recommendationHistoryId;
            this.feedbackAction = feedbackAction;
            this.rating = rating;
            this.reason = reason;
            this.comment = comment;
            this.isAnonymous = isAnonymous;
        }
    }

    /**
     * 암시적 피드백 제출 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ImplicitFeedbackRequest {
        private Long recommendationHistoryId; // 추천 이력 ID
        private FeedbackAction feedbackAction; // 액션 (CLICK, VIEW_DETAIL 등)

        @Builder
        public ImplicitFeedbackRequest(Long recommendationHistoryId, FeedbackAction feedbackAction) {
            this.recommendationHistoryId = recommendationHistoryId;
            this.feedbackAction = feedbackAction;
        }
    }

    /**
     * 피드백 응답 DTO
     */
    @Getter
    @Builder
    public static class FeedbackResponse {
        private Long feedbackId;
        private Long recommendationHistoryId;
        private FeedbackType feedbackType;
        private FeedbackAction feedbackAction;
        private Integer rating;
        private String reason;
        private String comment;
        private boolean isAnonymous;
        private double feedbackScore;
        private LocalDateTime createdAt;

        // 연관된 추천 정보
        private String recommendationType; // PROJECT, MEMBER
        private Long recommendedProjectId;
        private String recommendedProjectTitle;
        private Long recommendedUserId;
        private String recommendedUserName;
        private Double matchScore;
    }

    /**
     * 피드백 통계 DTO
     */
    @Getter
    @Builder
    public static class FeedbackStats {
        private long totalFeedbacks;
        private long positiveFeedbacks;
        private long negativeFeedbacks;
        private long neutralFeedbacks;
        private double positiveRate;
        private double averageRating;
        
        // 액션별 통계
        private long clicks;
        private long applies;
        private long invites;
        private long thumbsUp;
        private long thumbsDown;
        private long hides;
    }

    /**
     * API 공통 응답 DTO
     */
    @Getter
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String errorCode;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message, String errorCode) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .errorCode(errorCode)
                    .build();
        }
    }
}