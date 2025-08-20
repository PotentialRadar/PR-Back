package com.potential_radar.PR.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential_radar.PR.recommendation.dto.RecommendProjectRequest;
import com.potential_radar.PR.recommendation.dto.RecommendedProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectRecommendationService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
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
                log.warn("⚠️ AI 서버 응답이 비어있음 - Mock 데이터 사용");
                return getMockRecommendedProjects(topN);
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
            log.error("❌ AI 서버 호출 실패 - Mock 데이터로 대체: ", e);
            return getMockRecommendedProjects(topN);
        }
    }
    
    public List<RecommendedProject> getPopularProjects(int limit) {
        // 실제로는 DB에서 viewCount 기준으로 정렬하여 조회
        log.info("🔥 인기 프로젝트 조회 - 상위 {}개", limit);
        
        // 임시 Mock 데이터 - 실제로는 ProjectRepository에서 조회
        return getMockPopularProjects(limit);
    }
    
    public List<RecommendedProject> getMockRecommendedProjects(int limit) {
        Random random = new Random();
        return Arrays.asList(
            RecommendedProject.builder()
                .projectId(1L)
                .title("React 기반 쇼핑몰 개발")
                .description("TypeScript와 React를 활용한 현대적인 이커머스 플랫폼 개발")
                .matchScore(0.85)
                .projectTechStacks(Arrays.asList("React", "TypeScript", "Node.js"))
                .status("RECRUITING")
                .recruitDeadline("2025-08-25")
                .startDate("2025-09-01")
                .endDate("2025-12-15")
                .recruitCount(3)
                .appliedCount(random.nextInt(10))
                .viewCount(random.nextInt(200) + 50)
                .explanation(RecommendedProject.ProjectExplanation.builder()
                    .mainReason("React와 TypeScript 경험이 프로젝트 요구사항과 완벽하게 일치합니다")
                    .matchedSkills(Arrays.asList("React", "TypeScript"))
                    .growthOpportunities(Arrays.asList("Node.js 백엔드 경험", "팀 협업"))
                    .simpleExplanation("당신의 프론트엔드 실력을 활용하면서 풀스택 경험을 쌓을 수 있어요")
                    .difficultyLevel("intermediate")
                    .learningPotential(0.8)
                    .build())
                .build(),
                
            RecommendedProject.builder()
                .projectId(2L)
                .title("Vue3 + Nuxt 포트폴리오 사이트")
                .description("Nuxt3와 Vue3 Composition API를 활용한 개인 포트폴리오 웹사이트")
                .matchScore(0.78)
                .projectTechStacks(Arrays.asList("Vue.js", "Nuxt.js", "TypeScript"))
                .status("RECRUITING")
                .recruitDeadline("2025-08-30")
                .startDate("2025-09-10")
                .endDate("2025-11-30")
                .recruitCount(2)
                .appliedCount(random.nextInt(8))
                .viewCount(random.nextInt(150) + 30)
                .explanation(RecommendedProject.ProjectExplanation.builder()
                    .mainReason("Vue.js 경험을 통해 새로운 프론트엔드 기술을 학습할 기회")
                    .matchedSkills(Arrays.asList("TypeScript", "Frontend"))
                    .growthOpportunities(Arrays.asList("Vue.js 생태계", "SSR 경험"))
                    .simpleExplanation("TypeScript 지식을 활용해 Vue.js를 배우며 기술 영역을 확장할 수 있어요")
                    .difficultyLevel("beginner")
                    .learningPotential(0.9)
                    .build())
                .build(),
                
            RecommendedProject.builder()
                .projectId(3L)
                .title("Django REST API 서버 구축")
                .description("Python Django를 활용한 RESTful API 서버와 관리자 페이지 개발")
                .matchScore(0.65)
                .projectTechStacks(Arrays.asList("Python", "Django", "PostgreSQL"))
                .status("RECRUITING")
                .recruitDeadline("2025-09-01")
                .startDate("2025-09-15")
                .endDate("2025-12-01")
                .recruitCount(4)
                .appliedCount(random.nextInt(12))
                .viewCount(random.nextInt(180) + 40)
                .explanation(RecommendedProject.ProjectExplanation.builder()
                    .mainReason("백엔드 개발 경험을 쌓으며 풀스택 개발자로 성장할 기회")
                    .matchedSkills(Arrays.asList("API 설계", "Database"))
                    .growthOpportunities(Arrays.asList("Python 백엔드", "RESTful API 설계", "데이터베이스 관리"))
                    .simpleExplanation("프론트엔드 경험을 바탕으로 백엔드 개발을 배워 풀스택 역량을 키울 수 있어요")
                    .difficultyLevel("intermediate")
                    .learningPotential(0.85)
                    .build())
                .build()
        ).subList(0, Math.min(limit, 3));
    }
    
    public List<RecommendedProject> getMockPopularProjects(int limit) {
        Random random = new Random();
        return Arrays.asList(
            RecommendedProject.builder()
                .projectId(4L)
                .title("Spring Boot 마이크로서비스")
                .description("MSA 아키텍처 기반 Spring Boot 서비스 개발 및 Docker 컨테이너화")
                .matchScore(null) // 인기 프로젝트는 매칭 점수 없음
                .projectTechStacks(Arrays.asList("Java", "Spring Boot", "Docker"))
                .status("RECRUITING")
                .recruitDeadline("2025-08-22")
                .startDate("2025-09-05")
                .endDate("2026-01-15")
                .recruitCount(5)
                .appliedCount(random.nextInt(15) + 5)
                .viewCount(320) // 높은 조회수
                .explanation(null) // 인기 프로젝트는 설명 없음
                .build(),
                
            RecommendedProject.builder()
                .projectId(5L)
                .title("MSA 기반 클라우드 인프라 구축")
                .description("마이크로서비스 아키텍처와 컨테이너 기반 인프라 구축")
                .matchScore(null)
                .projectTechStacks(Arrays.asList("Docker", "Kubernetes", "AWS"))
                .status("RECRUITING")
                .recruitDeadline("2025-09-15")
                .startDate("2025-10-01")
                .endDate("2025-12-10")
                .recruitCount(5)
                .appliedCount(random.nextInt(20) + 8)
                .viewCount(295)
                .explanation(null)
                .build(),
                
            RecommendedProject.builder()
                .projectId(6L)
                .title("AI 챗봇 개발")
                .description("자연어 처리 기반 AI 챗봇 개발 프로젝트")
                .matchScore(null)
                .projectTechStacks(Arrays.asList("Python", "NLP", "TensorFlow"))
                .status("RECRUITING")
                .recruitDeadline("2025-09-20")
                .startDate("2025-10-05")
                .endDate("2025-12-30")
                .recruitCount(2)
                .appliedCount(random.nextInt(12) + 3)
                .viewCount(278)
                .explanation(null)
                .build()
        ).subList(0, Math.min(limit, 3));
    }
}