package com.potential_radar.PR.recommendation.controller;

import com.potential_radar.PR.recommendation.dto.RecommendedMemberResponse;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendation")
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * [팀원 대상] 사용자에게 적합한 프로젝트 목록을 추천
     * @param userId 추천 대상 사용자 ID
     * @return 추천된 프로젝트 리스트 (유사도 기반 정렬)
     */
    @GetMapping("/projects")
    public ResponseEntity<List<RecommendedProjectResponse>> recommendProjects(@RequestParam Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendedProjectsForUser(userId));
    }

    /**
     * [팀장 대상] 특정 프로젝트에 적합한 멤버 목록을 추천
     * @param projectId 추천 대상 프로젝트 ID
     * @return 추천된 멤버 리스트 (유사도 기반 정렬)
     */
    @GetMapping("/members")
    public ResponseEntity<List<RecommendedMemberResponse>> recommendMembers(@RequestParam Long projectId) {
        return ResponseEntity.ok(recommendationService.getRecommendedMembersForProject(projectId));
    }
}
