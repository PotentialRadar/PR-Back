package com.potential_radar.PR.recommendation.domain;

/**
 * 추천 피드백 타입
 */
public enum FeedbackType {
    POSITIVE("긍정적"),     // 좋아요, 관심 있음, 유용함
    NEGATIVE("부정적"),     // 싫어요, 관심 없음, 숨기기
    NEUTRAL("중립적");      // 보통, 기타

    private final String description;

    FeedbackType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}