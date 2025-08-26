package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.myPortfolio.OfficialPotfolioResponse;

public interface PublicPortfolioService {
    
    /**
     * 공개된 포트폴리오 조회
     * @param portfolioId 사용자 ID (포트폴리오 ID)
     * @return 공개된 포트폴리오 정보
     * @throws IllegalArgumentException 포트폴리오가 비공개이거나 존재하지 않는 경우
     */
    OfficialPotfolioResponse getPublicPortfolio(Long portfolioId);
}