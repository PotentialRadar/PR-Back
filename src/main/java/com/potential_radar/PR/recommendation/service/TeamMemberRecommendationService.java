package com.potential_radar.PR.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential_radar.PR.recommendation.domain.RecommendationHistory;
import com.potential_radar.PR.recommendation.domain.RecommendationType;
import com.potential_radar.PR.recommendation.dto.RecommendMemberRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedMember;
import com.potential_radar.PR.recommendation.repository.RecommendationHistoryRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamMemberRecommendationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final UserRepository userRepository;

    @Value("${python.api.host:http://localhost:8000}")
    private String aiServerUrl;
    
    // AI 모델 버전을 명시적으로 관리합니다.
    private static final String CURRENT_MODEL_VERSION = "1.0-hybrid";

    public List<RecommendedMember> recommendTeamMembers(RecommendMemberRequest request) {
        log.info("🚀 AI 서버 호출 시작 - URL: {}/api/recommend/members", aiServerUrl);
        log.info("🔍 요청 데이터 확인: projectId={}, requiredSkills={}, teamSize={}", 
                request.getProjectId(), request.getRequiredSkills(), request.getTeamSize());
        
        // null 값 검증 및 기본값 설정
        if (request.getProjectId() == null) {
            log.warn("⚠️ projectId가 null입니다. 기본값 1로 설정");
            request.setProjectId(1L);
        }
        if (request.getRequiredSkills() == null || request.getRequiredSkills().isEmpty()) {
            log.warn("⚠️ requiredSkills가 null이거나 비어있습니다. 기본값 설정");
            request.setRequiredSkills(List.of("React", "Node.js", "JavaScript"));
        }
        
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
            
            // 팀원 추천 이력을 DB에 저장
            saveTeamMemberRecommendationHistory(request, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ 팀원 추천 서비스 오류: {}", e.getMessage(), e);
            throw new RuntimeException("팀원 추천 서비스에 일시적인 문제가 발생했습니다", e);
        }
    }
    
    /**
     * 팀원 추천 이력을 DB에 저장
     */
    private void saveTeamMemberRecommendationHistory(RecommendMemberRequest request, List<RecommendedMember> recommendedMembers) {
        try {
            // 추천을 요청한 사용자 확인 (프로젝트 팀장)
            // 현재 request에는 teamLeaderId가 없으므로 추후 추가 필요
            // 임시로 excludeUserId를 팀장으로 간주
            Long teamLeaderId = request.getExcludeUserId(); 
            if (teamLeaderId == null) {
                log.warn("⚠️ 팀장 ID를 알 수 없어 팀원 추천 이력 저장을 건너뜁니다.");
                return;
            }
            
            User teamLeader = userRepository.findById(teamLeaderId).orElse(null);
            if (teamLeader == null) {
                log.warn("⚠️ 팀장 사용자를 찾을 수 없습니다. (ID: {})", teamLeaderId);
                return;
            }

            List<RecommendationHistory> histories = new ArrayList<>();
            
            for (RecommendedMember member : recommendedMembers) {
                // 추천된 팀원의 사용자 엔티티 조회
                User recommendedUser = userRepository.findById(member.getUserId()).orElse(null);
                
                if (recommendedUser == null) {
                    log.warn("⚠️ 추천된 사용자를 찾을 수 없습니다. (ID: {})", member.getUserId());
                    continue;
                }
                
                RecommendationHistory history = RecommendationHistory.builder()
                        .user(teamLeader) // 추천을 받은 사람 (팀장)
                        .recommendationType(RecommendationType.MEMBER) // 팀원 추천 타입
                        .recommendedProject(null) // 팀원 추천이므로 null
                        .recommendedMember(recommendedUser) // 추천된 팀원
                        .projectContextId(request.getProjectId()) // 어떤 프로젝트를 위한 추천인지
                        .matchScore(member.getMatchScore())
                        .modelVersion(CURRENT_MODEL_VERSION)
                        .build();
                        
                histories.add(history);
                log.debug("✅ 팀원 추천 이력 생성: 팀장 {} → 추천 팀원 {} (점수: {})", 
                    teamLeaderId, member.getName(), member.getMatchScore());
            }

            if (!histories.isEmpty()) {
                recommendationHistoryRepository.saveAll(histories);
                log.info("✅ {}개의 팀원 추천 이력을 저장했습니다. (팀장 ID: {}, 프로젝트 ID: {})", 
                    histories.size(), teamLeaderId, request.getProjectId());
            } else {
                log.warn("⚠️ 저장할 팀원 추천 이력이 없습니다.");
            }
            
        } catch (Exception e) {
            log.error("❌ 팀원 추천 이력 저장 실패: {}", e.getMessage(), e);
            // 이력 저장 실패가 전체 추천을 방해하지 않도록 예외를 던지지 않음
        }
    }
}