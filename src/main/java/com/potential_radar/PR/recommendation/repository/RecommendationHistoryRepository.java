package com.potential_radar.PR.recommendation.repository;

import com.potential_radar.PR.recommendation.domain.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {
    // 필요에 따라 추가적인 쿼리 메서드를 정의
}