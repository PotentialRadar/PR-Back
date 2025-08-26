package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.user.dto.portfolios.PortfolioListResponse;
import com.potential_radar.PR.user.dto.portfolios.PortfolioSearchRequest;
import com.potential_radar.PR.user.service.PortfolioListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class PortfolioListController {
    
    private final PortfolioListService portfolioListService;
    
    /**
     * 공개된 포트폴리오 목록 조회 (검색 및 페이징 포함)
     * GET /api/portfolios
     * 
     * @param techPart 기술 파트 필터 (선택사항)
     * @param experienceRange 경력 범위 필터 (선택사항)
     * @param keyword 검색 키워드 (선택사항)
     * @param sortBy 정렬 기준 (reputation, reviewCount, recent) - 기본값: reputation
     * @param page 페이지 번호 (0부터 시작) - 기본값: 0
     * @param size 페이지 크기 - 기본값: 12
     * @return 포트폴리오 목록과 페이징 정보
     */
    @GetMapping
    public ResponseEntity<PortfolioListResponse> getPortfolios(
            @RequestParam(required = false) String techPart,
            @RequestParam(required = false) ExperienceRange experienceRange,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "reputation") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        try {
            log.info("포트폴리오 목록 조회 API 호출: techPart={}, experienceRange={}, keyword={}, sortBy={}, page={}, size={}", 
                    techPart, experienceRange, keyword, sortBy, page, size);
            
            PortfolioSearchRequest searchRequest = PortfolioSearchRequest.builder()
                    .techPart(techPart)
                    .experienceRange(experienceRange)
                    .keyword(keyword)
                    .sortBy(sortBy)
                    .page(page)
                    .size(size)
                    .build();
            
            PortfolioListResponse response = portfolioListService.getPublicPortfolios(searchRequest);
            
            log.info("포트폴리오 목록 조회 API 성공: 총 {}개, 현재 페이지 {}/{}", 
                    response.totalElements(), response.currentPage() + 1, response.totalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("포트폴리오 목록 조회 중 서버 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 기술 파트별 포트폴리오 목록 조회 (간단한 경로)
     * GET /api/portfolios/tech/{techPartName}
     */
    @GetMapping("/tech/{techPartName}")
    public ResponseEntity<PortfolioListResponse> getPortfoliosByTechPart(
            @PathVariable String techPartName,
            @RequestParam(defaultValue = "reputation") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        try {
            log.info("기술 파트별 포트폴리오 목록 조회: techPart={}, page={}, size={}", techPartName, page, size);
            
            PortfolioSearchRequest searchRequest = PortfolioSearchRequest.builder()
                    .techPart(techPartName)
                    .sortBy(sortBy)
                    .page(page)
                    .size(size)
                    .build();
            
            PortfolioListResponse response = portfolioListService.getPublicPortfolios(searchRequest);
            
            log.info("기술 파트별 포트폴리오 목록 조회 성공: {} 파트 {}개", techPartName, response.totalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("기술 파트별 포트폴리오 목록 조회 중 서버 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}