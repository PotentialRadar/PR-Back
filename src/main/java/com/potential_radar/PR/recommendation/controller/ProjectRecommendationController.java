package com.potential_radar.PR.recommendation.controller;

import com.potential_radar.PR.recommendation.dto.RecommendProjectRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProject;
import com.potential_radar.PR.recommendation.service.ProjectRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectRecommendationController {
    
    private final ProjectRecommendationService projectRecommendationService;
    
    @PostMapping("/projects-for-user")
    public ResponseEntity<List<RecommendedProject>> recommendProjects(
            @RequestBody RecommendProjectRequest request,
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(defaultValue = "0.0") double minScore,
            @RequestParam(defaultValue = "0.1") double minOverlap,
            @RequestParam(defaultValue = "false") boolean strict
    ) {
        log.info("🎯 프로젝트 추천 요청 - 사용자 ID: {}, 기술스택: {}", 
                request.getUserId(), 
                request.getTechStacks().size() + "개");
        
        try {
            List<RecommendedProject> recommendations = projectRecommendationService
                    .recommendProjects(request, topN, minScore, minOverlap, strict);
            
            log.info("✅ 프로젝트 추천 완료 - {}개 추천", recommendations.size());
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            log.error("❌ 프로젝트 추천 실패: ", e);
            return ResponseEntity.ok(projectRecommendationService.getMockRecommendedProjects(topN));
        }
    }
    
    @GetMapping("/projects-for-user/popular")
    public ResponseEntity<List<RecommendedProject>> getPopularProjects(
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("🔥 인기 프로젝트 조회 요청 - 상위 {}개", limit);
        
        try {
            List<RecommendedProject> popularProjects = projectRecommendationService.getPopularProjects(limit);
            log.info("✅ 인기 프로젝트 조회 완료 - {}개", popularProjects.size());
            return ResponseEntity.ok(popularProjects);
            
        } catch (Exception e) {
            log.error("❌ 인기 프로젝트 조회 실패: ", e);
            return ResponseEntity.ok(projectRecommendationService.getMockRecommendedProjects(limit));
        }
    }
}