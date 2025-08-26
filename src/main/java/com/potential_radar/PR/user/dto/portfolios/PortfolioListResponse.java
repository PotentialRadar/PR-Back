package com.potential_radar.PR.user.dto.portfolios;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 포트폴리오 목록 조회 응답 DTO (페이징 정보 포함)
 */
public record PortfolioListResponse(
        List<PortfolioSummaryResponse> portfolios,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
    public static PortfolioListResponse from(Page<PortfolioSummaryResponse> page) {
        return new PortfolioListResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}