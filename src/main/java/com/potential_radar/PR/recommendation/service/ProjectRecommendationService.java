package com.potential_radar.PR.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.repository.ProjectApplicationRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.recommendation.dto.RecommendProjectRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectRecommendationService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    
    @Value("${python.api.host:http://localhost:8000}")
    private String aiServerUrl;
    
    public List<RecommendedProject> recommendProjects(
            RecommendProjectRequest request, 
            int topN, 
            double minScore, 
            double minOverlap, 
            boolean strict) {
        
        log.info("🚀 AI 서버 호출 시작 - URL: {}/api/recommend/projects", aiServerUrl);
        
        try {
            String jsonResponse = webClient.post()
                    .uri(aiServerUrl + "/api/recommend/projects")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("📦 AI 서버 응답 수신 - 길이: {}", jsonResponse != null ? jsonResponse.length() : 0);
            log.info("🔍 AI 서버 응답 내용: {}", jsonResponse);
            
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("⚠️ AI 서버 응답이 비어있음");
                throw new RuntimeException("AI 서버로부터 응답을 받을 수 없습니다");
            }
            
            List<RecommendedProject> result = objectMapper.readValue(
                jsonResponse, new TypeReference<List<RecommendedProject>>() {}
            );
            
            log.info("✅ AI 추천 완료 - {}개 프로젝트 추천", result.size());
            for (RecommendedProject project : result) {
                log.info("🔍 파싱된 프로젝트: ID={}, 제목={}, recruitCount={}, appliedCount={}, recruitDeadline={}", 
                    project.getProjectId(), project.getTitle(), project.getRecruitCount(), 
                    project.getAppliedCount(), project.getRecruitDeadline());
            }
            return result;
            
        } catch (Exception e) {
            log.error("❌ AI 서버 호출 실패: ", e);
            throw new RuntimeException("AI 추천 서비스에 일시적인 문제가 발생했습니다", e);
        }
    }
    
    public List<RecommendedProject> getPopularProjects(int limit) {
        log.info("🔥 인기 프로젝트 조회 - 상위 {}개", limit);
        
        try {
            // DB에서 viewCount 기준으로 정렬하여 조회
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"));
            List<ProjectRecruitment> popularProjects = projectRecruitmentRepository.findAll(pageRequest).getContent();
            
            return popularProjects.stream()
                .map(this::convertToRecommendedProject)
                .toList();
                
        } catch (Exception e) {
            log.error("❌ 인기 프로젝트 조회 실패: ", e);
            throw new RuntimeException("인기 프로젝트 조회 서비스에 일시적인 문제가 발생했습니다", e);
        }
    }
    
    private RecommendedProject convertToRecommendedProject(ProjectRecruitment project) {
        // 지원자 수 계산
        int appliedCount = projectApplicationRepository.countByProject_ProjectId(project.getProjectId());
        
        // 기술스택 추출
        List<String> techStacks = project.getTechStacks().stream()
            .map(ts -> ts.getTechStack().getName())
            .toList();
        
        return RecommendedProject.builder()
            .projectId(project.getProjectId())
            .title(project.getTitle())
            .description(project.getDescription())
            .projectTechStacks(techStacks)
            .status(project.getStatus().name())
            .recruitDeadline(project.getRecruitDeadline() != null ? project.getRecruitDeadline().toString() : null)
            .startDate(project.getStartDate() != null ? project.getStartDate().toString() : null)
            .endDate(project.getEndDate() != null ? project.getEndDate().toString() : null)
            .recruitCount(project.getRecruitCount())
            .appliedCount(appliedCount)
            .viewCount(project.getViewCount())
            .build();
    }
}