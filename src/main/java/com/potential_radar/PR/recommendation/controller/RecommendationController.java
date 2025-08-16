package com.potential_radar.PR.recommendation.controller;

import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 특정 사용자에게 적합한 프로젝트를 추천
     * POST 요청으로 /api/v1/recommend/projects 경로로 접근
     *
     * @param request 사용자 정보가 담긴 RecommendRequest 객체
     * @return 추천된 프로젝트 목록을 담은 HTTP 응답 (상태 코드 200 OK)
     */
//    @PostMapping("/projects")
//    public ResponseEntity<List<RecommendedProjectResponse>> getRecommendedProjectsForUser(@RequestBody RecommendRequest request) {
//        // RecommendationService를 호출하여 Python API로부터 추천된 프로젝트 목록을 가져옴
//        List<RecommendedProjectResponse> recommendedProjects = recommendationService.getRecommendedProjectsForUser(request);
//
//        // 추천 결과를 JSON 형태로 반환
//        return ResponseEntity.ok(recommendedProjects);
//    }

    @PostMapping("/projects")
    public ResponseEntity<List<RecommendedProjectResponse>> getRecommendedProjectsForUser(
            @RequestBody RecommendRequest request,
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(defaultValue = "0.5") double minScore,
            @RequestParam(defaultValue = "0.2") double minOverlap,
            @RequestParam(defaultValue = "false") boolean strict
    ) {
        List<RecommendedProjectResponse> result =
                recommendationService.getRecommendedProjectsForUser(request, topN, minScore, minOverlap, strict);
        
        // Explanation 데이터 확인 로깅
        for (RecommendedProjectResponse project : result) {
            if (project.getExplanation() != null) {
                System.out.println("✅ 프로젝트 " + project.getProjectId() + " explanation 있음: " + 
                    project.getExplanation().getMainReason());
            } else {
                System.out.println("❌ 프로젝트 " + project.getProjectId() + " explanation 없음");
            }
        }
        
        return ResponseEntity.ok(result);
    }
}
