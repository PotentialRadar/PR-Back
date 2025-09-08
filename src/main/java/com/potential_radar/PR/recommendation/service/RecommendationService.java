package com.potential_radar.PR.recommendation.service;

import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectTechStack;
import com.potential_radar.PR.project.repository.ProjectApplicationRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.recommendation.domain.FeedbackAction;
import com.potential_radar.PR.recommendation.domain.RecommendationFeedback;
import com.potential_radar.PR.recommendation.domain.RecommendationHistory;
import com.potential_radar.PR.recommendation.domain.RecommendationType;
import com.potential_radar.PR.recommendation.dto.LikedProject;
import com.potential_radar.PR.recommendation.dto.RecommendRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProjectResponse;
import com.potential_radar.PR.recommendation.repository.RecommendationFeedbackRepository;
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
import java.util.Map;

@Slf4j
@Service
public class RecommendationService {

    private final WebClient webClient;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final RecommendationFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final LikeRepository likeRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final ProjectTechStackRepository projectTechStackRepository;

    // AI 모델 버전을 명시적으로 관리합니다.
    private static final String CURRENT_MODEL_VERSION = "1.0-hybrid";

    public RecommendationService(WebClient.Builder webClientBuilder,
                                 @Value("${python.api.host:http://localhost:8000}") String pythonApiHost,
                                 RecommendationHistoryRepository recommendationHistoryRepository,
                                 RecommendationFeedbackRepository feedbackRepository,
                                 UserRepository userRepository,
                                 ProjectRecruitmentRepository projectRecruitmentRepository,
                                 ProjectApplicationRepository projectApplicationRepository,
                                 LikeRepository likeRepository,
                                 UserTechStackRepository userTechStackRepository, ProjectTechStackRepository projectTechStackRepository) {
        this.recommendationHistoryRepository = recommendationHistoryRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.projectRecruitmentRepository = projectRecruitmentRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.likeRepository = likeRepository;
        this.userTechStackRepository = userTechStackRepository;
        this.projectTechStackRepository = projectTechStackRepository;
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
        try {
            // 추천받은 사용자의 엔티티를 조회합니다.
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

            List<RecommendationHistory> histories = new ArrayList<>();
            for (RecommendedProjectResponse res : responses) {
                // 추천된 프로젝트의 엔티티를 조회합니다.
                ProjectRecruitment project = projectRecruitmentRepository.findById(res.getProjectId())
                        .orElse(null);
                        
                if (project == null) {
                    log.warn("⚠️ 프로젝트 ID {}를 찾을 수 없습니다. 추천 이력에서 제외", res.getProjectId());
                    continue;
                }
                
                RecommendationHistory history = RecommendationHistory.builder()
                        .user(user)
                        .recommendationType(RecommendationType.PROJECT) // 명시적으로 프로젝트 추천 타입 설정
                        .recommendedProject(project)
                        .recommendedMember(null) // 프로젝트 추천이므로 null
                        .projectContextId(null) // 프로젝트 추천이므로 null
                        .matchScore(res.getMatchScore())
                        .modelVersion(CURRENT_MODEL_VERSION)
                        .build();
                        
                histories.add(history);
                log.debug("✅ 프로젝트 추천 이력 생성: 사용자 {} → 프로젝트 {} (점수: {})", 
                    userId, project.getTitle(), res.getMatchScore());
            }

            if (!histories.isEmpty()) {
                List<RecommendationHistory> savedHistories = recommendationHistoryRepository.saveAll(histories);
                
                // 저장된 이력 ID를 응답 DTO에 설정
                for (int i = 0; i < savedHistories.size() && i < responses.size(); i++) {
                    responses.get(i).setRecommendationHistoryId(savedHistories.get(i).getId());
                }
                
                log.info("✅ {}개의 프로젝트 추천 이력을 저장했습니다. (사용자 ID: {})", savedHistories.size(), userId);
            } else {
                log.warn("⚠️ 저장할 프로젝트 추천 이력이 없습니다. (사용자 ID: {})", userId);
            }
            
        } catch (Exception e) {
            log.error("❌ 프로젝트 추천 이력 저장 실패 (사용자 ID: {}): {}", userId, e.getMessage(), e);
            // 이력 저장 실패가 전체 추천을 방해하지 않도록 예외를 던지지 않음
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
                
                // 조회수 설정
                response.setViewCount(project.getViewCount());
                
                // 좋아요 수 계산
                int likeCount = (int) likeRepository.countByTargetTypeAndTargetId(TargetType.PROJECT, project.getProjectId());
                response.setLikeCount(likeCount);
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
    
    /**
     * 추천에 대한 간단한 피드백 저장
     */
    @Transactional
    public void saveFeedback(Long userId, Long recommendationHistoryId, FeedbackAction action) {
        try {
            // 사용자 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

            // 추천 이력 확인
            RecommendationHistory recommendationHistory = recommendationHistoryRepository
                    .findById(recommendationHistoryId)
                    .orElseThrow(() -> new EntityNotFoundException("추천 이력을 찾을 수 없습니다: " + recommendationHistoryId));

            // 권한 확인 - 추천받은 사용자만 피드백 가능
            if (!recommendationHistory.getUser().getUserId().equals(userId)) {
                throw new IllegalArgumentException("본인의 추천에 대해서만 피드백할 수 있습니다.");
            }

            // 기존 피드백 확인 및 업데이트/생성
            RecommendationFeedback existingFeedback = feedbackRepository
                    .findByUserUserIdAndRecommendationHistoryId(userId, recommendationHistoryId)
                    .orElse(null);

            if (existingFeedback != null) {
                // 기존 피드백 업데이트 (soft update)
                log.info("🔄 기존 피드백 업데이트: 사용자 {} -> 추천 {} ({})", 
                        userId, recommendationHistoryId, action);
                // 기존 피드백을 삭제하고 새로 생성
                feedbackRepository.delete(existingFeedback);
            }

            // 새 피드백 생성 및 저장
            RecommendationFeedback feedback = RecommendationFeedback.builder()
                    .user(user)
                    .recommendationHistory(recommendationHistory)
                    .feedbackAction(action)
                    .build();

            feedbackRepository.save(feedback);

            log.info("✅ 피드백 저장 완료: 사용자 {} -> 추천 {} ({})", 
                    userId, recommendationHistoryId, action.getDescription());

        } catch (Exception e) {
            log.error("❌ 피드백 저장 실패 (사용자 {}, 추천 {}): {}", 
                    userId, recommendationHistoryId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 사용자 피드백 통계 조회 (AI 서버용)
     */
    public Map<String, Object> getUserFeedbackStats(Long userId) {
        try {
            // 사용자 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

            // 사용자의 모든 피드백 조회
            List<RecommendationFeedback> feedbacks = feedbackRepository
                    .findByUserUserIdOrderByCreatedAtDesc(userId);

            // 통계 계산
            long totalFeedbacks = feedbacks.size();
            long likeCount = feedbacks.stream()
                    .filter(f -> f.getFeedbackAction() == FeedbackAction.LIKE)
                    .count();
            long dislikeCount = feedbacks.stream()
                    .filter(f -> f.getFeedbackAction() == FeedbackAction.DISLIKE)
                    .count();

            double likeRatio = totalFeedbacks > 0 ? (double) likeCount / totalFeedbacks : 0.5;
            boolean hasEnoughData = totalFeedbacks >= 5; // 최소 5개 피드백이 있어야 신뢰 가능

            // 사용자의 좋아요한 프로젝트들의 기술스택 패턴 분석
            List<String> preferredTechStacks = feedbacks.stream()
                    .filter(f -> f.getFeedbackAction() == FeedbackAction.LIKE)
                    .map(f -> f.getRecommendationHistory().getRecommendedProject())
                    .filter(project -> project != null)
                    .flatMap(project -> {
                        try {
                            // 프로젝트의 기술스택 조회
                            List<ProjectTechStack> projectTechStacks = 
                                projectTechStackRepository.findByProject(project);
                            return projectTechStacks.stream()
                                .map(pts -> pts.getTechStack().getName().toLowerCase());
                        } catch (Exception e) {
                            log.warn("⚠️ 프로젝트 {} 기술스택 조회 실패: {}", 
                                project.getProjectId(), e.getMessage());
                            return java.util.stream.Stream.<String>empty();
                        }
                    })
                    .distinct()
                    .toList();

            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("userId", userId);
            stats.put("totalFeedbacks", totalFeedbacks);
            stats.put("likeCount", likeCount);
            stats.put("dislikeCount", dislikeCount);
            stats.put("likeRatio", Math.round(likeRatio * 100.0) / 100.0); // 소수점 2자리
            stats.put("hasEnoughData", hasEnoughData);
            stats.put("preferredTechStacks", preferredTechStacks);

            log.info("✅ 사용자 {} 피드백 통계: 총 {}개, 좋아요 {}개({}%), 선호 기술: {}", 
                    userId, totalFeedbacks, likeCount, Math.round(likeRatio * 100), 
                    preferredTechStacks.size() > 3 ? preferredTechStacks.subList(0, 3) + "..." : preferredTechStacks);

            return stats;

        } catch (Exception e) {
            log.error("❌ 사용자 피드백 통계 조회 실패 (사용자 ID: {}): {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 피드백 모달 표시 여부 결정 (세션 기반)
     * 세션당 한 번만 피드백 모달을 표시하도록 제어
     */
    public boolean shouldShowFeedbackModal(Long userId, String sessionId) {
        try {
            // 사용자 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

            // 세션 ID가 없으면 기본적으로 표시 안 함
            if (sessionId == null || sessionId.trim().isEmpty()) {
                log.warn("⚠️ 세션 ID가 없어서 피드백 모달을 표시하지 않습니다 (사용자: {})", userId);
                return false;
            }

            // 최근 30분 내에 해당 사용자가 같은 세션에서 피드백을 제공했는지 확인
            java.time.LocalDateTime thirtyMinutesAgo = java.time.LocalDateTime.now().minusMinutes(30);
            
            // 최근 피드백 중에 세션 정보가 있는지 확인 (실제로는 세션을 따로 저장하지 않으므로 시간 기반으로 판단)
            List<RecommendationFeedback> recentFeedbacks = feedbackRepository
                    .findByUserUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .filter(feedback -> {
                        java.time.LocalDateTime feedbackTime = feedback.getCreatedAt();
                        return feedbackTime != null && feedbackTime.isAfter(thirtyMinutesAgo);
                    })
                    .toList();

            boolean hasRecentFeedback = !recentFeedbacks.isEmpty();
            
            log.info("🔍 피드백 모달 표시 여부 확인 - 사용자 {}, 세션 {}: 최근 30분 피드백 {}개, 모달 표시 여부: {}", 
                    userId, sessionId, recentFeedbacks.size(), !hasRecentFeedback);

            // 최근 30분 내에 피드백을 제공하지 않았으면 모달 표시
            return !hasRecentFeedback;

        } catch (Exception e) {
            log.error("❌ 피드백 모달 표시 여부 확인 실패 (사용자 ID: {}, 세션: {}): {}", userId, sessionId, e.getMessage(), e);
            // 에러 시 안전하게 모달을 표시하지 않음
            return false;
        }
    }

    /**
     * 사용자가 숨김 처리한 프로젝트 ID 목록 조회 (AI 서버용)
     */
    public List<Long> getHiddenProjectIds(Long userId) {
        try {
            // 사용자 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));

            // 숨김 처리한 피드백들에서 프로젝트 ID 추출
            List<Long> hiddenProjectIds = feedbackRepository
                    .findByUserUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .filter(feedback -> feedback.getFeedbackAction() == FeedbackAction.HIDE)
                    .map(feedback -> feedback.getRecommendationHistory().getRecommendedProject().getProjectId())
                    .distinct()
                    .toList();

            log.info("🔍 사용자 {} 숨김 프로젝트 조회: {}개", userId, hiddenProjectIds.size());
            return hiddenProjectIds;

        } catch (Exception e) {
            log.error("❌ 숨김 프로젝트 조회 실패 (사용자 ID: {}): {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
