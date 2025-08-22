package com.potential_radar.PR.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential_radar.PR.recommendation.dto.RecommendMemberRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamMemberRecommendationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${python.api.host:http://localhost:8000}")
    private String aiServerUrl;

    public List<RecommendedMember> recommendTeamMembers(RecommendMemberRequest request) {
        log.info("🚀 AI 서버 호출 시작 - URL: {}/api/recommend/members", aiServerUrl);
        
        try {
            String jsonResponse = webClient.post()
                    .uri(aiServerUrl + "/api/recommend/members")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .doOnSuccess(response -> log.info("✅ AI 서버 JSON 응답 받음: {}", response))
                    .doOnError(error -> log.error("❌ AI 서버 호출 실패: {}", error.getMessage()))
                    .block();

            List<RecommendedMember> result = objectMapper.readValue(
                jsonResponse, 
                new TypeReference<List<RecommendedMember>>() {}
            );
            
            log.info("✅ 파싱 완료 - 추천 팀원 수: {}", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 팀원 추천 서비스 오류: {}", e.getMessage(), e);
            
            // Fallback: 목업 데이터 반환
            log.warn("⚠️ Fallback: 목업 데이터로 응답");
            return getMockRecommendedMembers();
        }
    }


    private List<RecommendedMember> getMockRecommendedMembers() {
        // 임시 목업 데이터 - AI 서버 연동 실패시 사용
        return List.of(
                createMockMember(1L, "김개발자", 0.87),
                createMockMember(3L, "이백엔드", 0.73),
                createMockMember(4L, "정모바일", 0.65),
                createMockMember(5L, "최AI", 0.78)
        );
    }

    private RecommendedMember createMockMember(Long userId, String name, double matchScore) {
        RecommendedMember member = new RecommendedMember();
        member.setUserId(userId);
        member.setName(name);
        member.setMatchScore(matchScore);
        member.setEmail(name.toLowerCase() + "@example.com");
        member.setProfileImage("https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId);
        member.setExperience("3-5년");
        member.setIsAvailable(true);
        member.setCurrentProjectCount(1);
        member.setCompletedProjects(5);
        member.setAverageRating(4.5);
        member.setLastActiveDate("2025-08-18");
        return member;
    }
}