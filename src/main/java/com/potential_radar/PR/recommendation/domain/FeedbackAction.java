package com.potential_radar.PR.recommendation.domain;

/**
 * 추천에 대한 간단한 피드백 타입
 */
public enum FeedbackAction {
    LIKE("좋아요"),
    DISLIKE("별로에요");

    private final String description;

    FeedbackAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    
    /**
     * 긍정적 피드백인지 확인
     */
    public boolean isPositive() {
        return this == LIKE;
    }
}