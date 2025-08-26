package com.potential_radar.PR.recommendation.service;

import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.repository.ProjectApplicationRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.recommendation.domain.RecommendationHistory;
import com.potential_radar.PR.recommendation.dto.LikedProject;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.repository.RecommendationHistoryRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserTechStack;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
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
    private final LikeRepository likeRepository;
    private final UserTechStackRepository userTechStackRepository;

    // AI 모델 버전을 명시적으로 관리합니다.
    private static final String CURRENT_MODEL_VERSION = "1.0-hybrid";

    public RecommendationService(WebClient.Builder webClientBuilder,
                                 @Value("${python.api.host:http://localhost:8000}") String pythonApiHost,
                                 RecommendationHistoryRepository recommendationHistoryRepository,
                                 UserRepository userRepository,
                                 ProjectRecruitmentRepository projectRecruitmentRepository,
                                 ProjectApplicationRepository projectApplicationRepository,
                                 LikeRepository likeRepository,
                                 UserTechStackRepository userTechStackRepository) {
        this.pythonApiHost = pythonApiHost;
        this.recommendationHistoryRepository = recommendationHistoryRepository;
        this.userRepository = userRepository;
        this.projectRecruitmentRepository = projectRecruitmentRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.likeRepository = likeRepository;
        this.userTechStackRepository = userTechStackRepository;
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
            // 1. 사용자 기술스택 데이터 수집
            List<RecommendRequest.TechStackForAI> userTechStacks = getUserTechStacks(request.getUserId());
            request.setTechStacks(userTechStacks);
            log.info("🔧 사용자 기술스택 수집 완료: {}", userTechStacks);
            
            // 2. 좋아요 데이터 포함 여부 확인 및 수집  
            log.info("🔍 좋아요 데이터 포함 여부 확인: includeLikes={}", request.isIncludeLikes());
            if (request.isIncludeLikes()) {
                List<LikedProject> likedProjects = getUserLikedProjects(request.getUserId());
                request.setLikedProjects(likedProjects);
                log.info("📍 사용자 좋아요 데이터 수집 완료 - {}개 프로젝트", likedProjects.size());
                if (!likedProjects.isEmpty()) {
                    log.info("📍 첫 번째 좋아요 프로젝트: {}", likedProjects.get(0));
                }
            } else {
                log.info("📍 좋아요 데이터 사용 안 함 - 기술스택만 사용");
            }
            
            // AI 서버로 보내는 요청 데이터 로깅
            log.info("🚀 AI 서버로 보내는 요청 데이터: {}", request);
            
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
            log.error("FastAPI 응답 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // AI 서버 오류 시 빈 리스트 반환
            if (e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError()) {
                log.warn("AI 서버 오류로 인한 빈 리스트 반환");
                return Collections.emptyList();
            }
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

    /**
     * 사용자의 좋아요한 프로젝트 데이터 수집
     * BaseTimeEntity를 활용하여 시간 기반 분석 가능
     */
    private List<LikedProject> getUserLikedProjects(Long userId) {
        try {
            log.info("🔍 사용자 {}의 좋아요 데이터 수집 시작", userId);
            
            // 사용자 존재 여부 확인 (NotFoundException 활용)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            log.info("✅ 사용자 {} 확인됨: {}", userId, user.getEmail());
            
            // 프로젝트 좋아요 데이터 조회
            List<Like> projectLikes = likeRepository.findByUserAndTargetType(user, TargetType.PROJECT);
            
            log.info("🔍 사용자 {}의 프로젝트 좋아요 {}개 발견", userId, projectLikes.size());
            
            if (projectLikes.isEmpty()) {
                log.info("📍 사용자 {}는 좋아요한 프로젝트가 없습니다", userId);
                return List.of();
            }
            
            // Like를 LikedProject DTO로 변환
            List<LikedProject> likedProjects = projectLikes.stream()
                    .map(this::convertToLikedProject)
                    .filter(likedProject -> likedProject != null) // null 안전성
                    .toList();
                    
            log.info("🎯 사용자 {}의 좋아요 데이터 변환 완료: {}개 → {}개", userId, projectLikes.size(), likedProjects.size());
            return likedProjects;
                    
        } catch (Exception e) {
            log.warn("⚠️ 사용자 좋아요 데이터 수집 실패 (userId: {}): {}", userId, e.getMessage(), e);
            // 좋아요 데이터 수집 실패는 전체 추천을 중단시키지 않음
            return List.of();
        }
    }
    
    /**
     * Like 엔티티를 LikedProject DTO로 변환
     * BaseTimeEntity의 createdAt 활용
     */
    private LikedProject convertToLikedProject(Like like) {
        try {
            // 좋아요한 프로젝트 정보 조회
            ProjectRecruitment project = projectRecruitmentRepository.findById(like.getTargetId())
                    .orElse(null);
                    
            if (project == null) {
                log.warn("⚠️ 좋아요한 프로젝트를 찾을 수 없습니다: {}", like.getTargetId());
                return null;
            }
            
            // 프로젝트 기술스택 추출
            List<String> techStacks = project.getTechStacks().stream()
                    .map(ts -> ts.getTechStack().getName())
                    .toList();
            
            // 프로젝트 카테고리 추출 (TechPart 기반)
            String category = project.getTechParts().stream()
                    .findFirst()
                    .map(tp -> tp.getTechPart().getName())
                    .orElse("기타");
            
            return new LikedProject(
                    project.getProjectId(),
                    project.getTitle(),
                    techStacks,
                    like.getCreatedAt(), // BaseTimeEntity의 createdAt 활용
                    category
            );
            
        } catch (Exception e) {
            log.error("❌ Like를 LikedProject로 변환 실패 (likeId: {}): {}", like.getId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 사용자의 기술스택 데이터 수집
     * DB에서 사용자의 기술스택을 조회하여 이름 리스트로 변환
     */
    private List<RecommendRequest.TechStackForAI> getUserTechStacks(Long userId) {
        try {
            log.info("🔧 사용자 {}의 기술스택 데이터 수집 시작", userId);
            
            // 사용자 존재 여부 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            
            // 사용자 기술스택 조회
            List<UserTechStack> userTechStacks = userTechStackRepository.findByUserWithTechStack(user);
            
            log.info("🔍 사용자 {}의 기술스택 {}개 발견", userId, userTechStacks.size());
            
            if (userTechStacks.isEmpty()) {
                log.warn("⚠️ 사용자 {}는 등록된 기술스택이 없습니다", userId);
                return List.of();
            }
            
            // UserTechStack을 AI 서버용 DTO로 변환
            List<RecommendRequest.TechStackForAI> techStacksForAI = userTechStacks.stream()
                    .map(uts -> new RecommendRequest.TechStackForAI(
                        uts.getStack().getName().toLowerCase(), // Python에서 소문자로 처리
                        uts.getSkillLevel()
                    ))
                    .toList();
                    
            log.info("🎯 사용자 {}의 기술스택 변환 완료:", userId);
            techStacksForAI.forEach(tech -> 
                log.info("  - {} (레벨: {})", tech.getName(), tech.getLevel())
            );
            return techStacksForAI;
                    
        } catch (Exception e) {
            log.error("❌ 사용자 기술스택 데이터 수집 실패 (userId: {}): {}", userId, e.getMessage(), e);
            // 기술스택 데이터 수집 실패는 전체 추천을 중단시키지 않음
            return List.of();
        }
    }
}
