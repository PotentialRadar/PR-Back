package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.portfolios.PortfolioListResponse;
import com.potential_radar.PR.user.dto.portfolios.PortfolioSearchRequest;

public interface PortfolioListService {
    
    /**
     * 공개된 포트폴리오 목록을 조회합니다 (검색 및 페이징 포함)
     * @param searchRequest 검색 조건 및 페이징 정보
     * @return 포트폴리오 목록과 페이징 정보
     */
    PortfolioListResponse getPublicPortfolios(PortfolioSearchRequest searchRequest);
}