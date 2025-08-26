package com.potential_radar.PR.user.dto.portfolios;

import com.potential_radar.PR.user.domain.ExperienceRange;
import lombok.Builder;

/**
 * 포트폴리오 검색 및 필터링을 위한 요청 DTO
 */
@Builder
public record PortfolioSearchRequest(
        String techPart,           // 기술 파트 필터 (예: "BACKEND", "FRONTEND", "DEVOPS")
        ExperienceRange experienceRange, // 경력 범위 필터
        String keyword,            // 닉네임이나 자기소개에서 검색할 키워드
        String sortBy,             // 정렬 기준 ("reputation", "reviewCount", "recent")
        int page,                  // 페이지 번호 (0부터 시작)
        int size                   // 페이지 크기
) {
    public static PortfolioSearchRequest defaultRequest() {
        return PortfolioSearchRequest.builder()
                .page(0)
                .size(12)
                .sortBy("reputation")
                .build();
    }
    
    public PortfolioSearchRequest withDefaults() {
        return PortfolioSearchRequest.builder()
                .techPart(this.techPart)
                .experienceRange(this.experienceRange)
                .keyword(this.keyword)
                .sortBy(this.sortBy != null ? this.sortBy : "reputation")
                .page(Math.max(0, this.page))
                .size(this.size > 0 && this.size <= 100 ? this.size : 12)
                .build();
    }
}