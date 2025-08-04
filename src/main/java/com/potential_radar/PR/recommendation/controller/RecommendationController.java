package com.potential_radar.PR.recommendation.controller;

import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 특정 사용자에게 적합한 프로젝트를 추천합니다.
     * POST 요청으로 /api/v1/recommend/projects 경로로 접근합니다.
     *
     * @param request 사용자 정보가 담긴 RecommendRequest 객체
     * @return 추천된 프로젝트 목록을 담은 HTTP 응답 (상태 코드 200 OK)
     */
    @PostMapping("/projects")
    public ResponseEntity<List<RecommendedProjectResponse>> getRecommendedProjectsForUser(@RequestBody RecommendRequest request) {
        // RecommendationService를 호출하여 Python API로부터 추천된 프로젝트 목록을 가져옵니다.
        List<RecommendedProjectResponse> recommendedProjects = recommendationService.getRecommendedProjectsForUser(request);

        // 추천 결과를 JSON 형태로 반환
        return ResponseEntity.ok(recommendedProjects);
    }
}
