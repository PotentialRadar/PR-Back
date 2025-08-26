package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.myPortfolio.OfficialPotfolioResponse;
import com.potential_radar.PR.user.service.PublicPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class PublicPortfolioController {
    
    private final PublicPortfolioService publicPortfolioService;
    
    /**
     * 공개 포트폴리오 조회
     * GET /api/portfolio/{portfolio_id}
     * 
     * @param portfolioId 포트폴리오 ID (사용자 ID)
     * @return 공개된 포트폴리오 정보
     */
    @GetMapping("/{portfolio_id}")
    public ResponseEntity<?> getPublicPortfolio(@PathVariable("portfolio_id") Long portfolioId) {
        try {
            log.info("공개 포트폴리오 조회 API 호출: portfolio_id = {}", portfolioId);
            
            OfficialPotfolioResponse portfolio = publicPortfolioService.getPublicPortfolio(portfolioId);
            
            log.info("공개 포트폴리오 조회 API 성공: portfolio_id = {}, nickname = {}", 
                    portfolioId, portfolio.nickname());
            
            return ResponseEntity.ok(portfolio);
            
        } catch (IllegalArgumentException e) {
            log.warn("공개 포트폴리오 조회 실패: portfolio_id = {}, error = {}", portfolioId, e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("공개 포트폴리오 조회 중 서버 오류: portfolio_id = {}", portfolioId, e);
            return ResponseEntity.internalServerError()
                    .body("포트폴리오 조회 중 오류가 발생했습니다.");
        }
    }
}