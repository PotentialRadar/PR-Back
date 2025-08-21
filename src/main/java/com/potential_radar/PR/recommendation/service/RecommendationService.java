package com.potential_radar.PR.recommendation.service;

import com.potential_radar.PR.common.exception.RecommendationServiceException;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.repository.ProjectApplicationRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.recommendation.domain.RecommendationHistory;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.repository.RecommendationHistoryRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class RecommendationService {

    private final WebClient webClient;
    private final String pythonApiHost;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final UserRepository userRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;

    // AI 모델 버전을 명시적으로 관리합니다.
    private static final String CURRENT_MODEL_VERSION = "1.0-hybrid";

    public RecommendationService(WebClient.Builder webClientBuilder,
                                 @Value("${python.api.host:http://localhost:8000}") String pythonApiHost,
                                 RecommendationHistoryRepository recommendationHistoryRepository,
                                 UserRepository userRepository,
                                 ProjectRecruitmentRepository projectRecruitmentRepository,
                                 ProjectApplicationRepository projectApplicationRepository) {
        this.pythonApiHost = pythonApiHost;
        this.recommendationHistoryRepository = recommendationHistoryRepository;
        this.userRepository = userRepository;
        this.projectRecruitmentRepository = projectRecruitmentRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.webClient = webClientBuilder.baseUrl(pythonApiHost).build();
    }

    /**
     * Python API를 호출하여 사용자에게 적합한 프로젝트를 추천
     * WebClient를 사용하여 비동기적으로 POST 요청을 보냄
     *
     * @param request 사용자 정보가 담긴 RecommendRequest 객체
     * @return 추천된 프로젝트 목록 (API 호출 실패 시 빈 리스트 반환)
     */
    @Transactional
    public List<RecommendedProjectResponse> getRecommendedProjectsForUser(
            RecommendRequest request,
            int topN,
            double minScore,
            double minOverlap,
            boolean strict
    ) {
        log.info("[SPRING->AI] strict={} topN={} minScore={} minOverlap={}",
                strict, topN, minScore, minOverlap);

        try {
            // AI 서버 응답을 먼저 String으로 받아서 로그 출력
            String rawResponse = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/recommend/projects")
                            .queryParam("topN", topN)
                            .queryParam("minScore", minScore)
                            .queryParam("minOverlap", minOverlap)
                            .queryParam("strict", strict)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                    
            log.info("🔍 AI 서버 원본 응답: {}", rawResponse);
            
            // 이제 JSON을 객체로 변환
            List<RecommendedProjectResponse> recommendedProjects = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/recommend/projects")
                            .queryParam("topN", topN)
                            .queryParam("minScore", minScore)
                            .queryParam("minOverlap", minOverlap)
                            .queryParam("strict", strict)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(RecommendedProjectResponse.class)
                    .collectList()
                    .block();

            // AI 서버로부터 받은 추천 결과를 DB에 이력으로 저장
            if (recommendedProjects != null && !recommendedProjects.isEmpty()) {
                // 추가 프로젝트 정보 설정
                enrichProjectResponses(recommendedProjects);
                saveRecommendationHistories(request.getUserId(), recommendedProjects);
            }

            return recommendedProjects;
        } catch (WebClientResponseException e) {
            log.error("FastAPI 응답 오류: {}", e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("추천 서비스 예외: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private void saveRecommendationHistories(Long userId, List<RecommendedProjectResponse> responses) {
        // 추천받은 사용자의 엔티티를 조회합니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<RecommendationHistory> histories = new ArrayList<>();
        for (RecommendedProjectResponse res : responses) {
            // 추천된 프로젝트의 엔티티를 조회합니다.
            projectRecruitmentRepository.findById(res.getProjectId()).ifPresent(project -> {
                RecommendationHistory history = RecommendationHistory.builder()
                        .user(user)
                        .recommendedProject(project)
                        .matchScore(res.getMatchScore())
                        .modelVersion(CURRENT_MODEL_VERSION)
                        .build();
                histories.add(history);
            });
        }

        if (!histories.isEmpty()) {
            recommendationHistoryRepository.saveAll(histories);
            log.info("{}개의 추천 이력을 저장했습니다. (사용자 ID: {})", histories.size(), userId);
        }
    }

    /**
     * 추천된 프로젝트 목록에 추가 정보 설정
     */
    private void enrichProjectResponses(List<RecommendedProjectResponse> responses) {
        for (RecommendedProjectResponse response : responses) {
            projectRecruitmentRepository.findById(response.getProjectId()).ifPresent(project -> {
                // 기본 프로젝트 정보 설정
                response.setStartDate(project.getStartDate());
                response.setEndDate(project.getEndDate());
                response.setRecruitCount(project.getRecruitCount());
                response.setRecruitDeadline(project.getRecruitDeadline());
                
                // 지원자 수 계산
                int appliedCount = projectApplicationRepository.countByProject_ProjectId(project.getProjectId());
                response.setAppliedCount(appliedCount);
            });
        }
    }
}
