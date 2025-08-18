package com.potential_radar.PR.recommendation.controller;

import com.potential_radar.PR.recommendation.dto.RecommendMemberRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedMember;
import com.potential_radar.PR.recommendation.service.TeamMemberRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 개발용, 실제 배포시에는 특정 도메인으로 제한
public class TeamMemberRecommendationController {

    private final TeamMemberRecommendationService recommendationService;

    @PostMapping("/members")
    public ResponseEntity<List<RecommendedMember>> recommendMembers(
            @RequestBody RecommendMemberRequest request) {
        
        log.info("🤖 팀원 추천 요청 - 프로젝트 ID: {}, 필요 기술: {}, 팀 크기: {}", 
                request.getProjectId(), request.getRequiredSkills(), request.getTeamSize());
        
        try {
            List<RecommendedMember> recommendedMembers = recommendationService.recommendTeamMembers(request);
            
            log.info("✅ 팀원 추천 완료 - 추천된 팀원 수: {}", recommendedMembers.size());
            
            return ResponseEntity.ok(recommendedMembers);
            
        } catch (Exception e) {
            log.error("❌ 팀원 추천 실패: {}", e.getMessage(), e);
            throw new RuntimeException("팀원 추천 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}