package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {
    
    private static final DateTimeFormatter ELASTICSEARCH_DATE_FORMAT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    private final UserRepository userRepository;
    private final UserSearchRepository userSearchRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectSearchRepository projectSearchRepository;
    
    @Transactional(readOnly = true)
    public void syncAllUsersToElasticsearch() {
        log.info("Starting user data synchronization to Elasticsearch...");
        
        List<User> users = userRepository.findAll();
        List<UserSearchDocument> userDocs = users.stream()
                .map(this::convertUserToDocument)
                .collect(Collectors.toList());
        
        userSearchRepository.saveAll(userDocs);
        log.info("Synchronized {} users to Elasticsearch", userDocs.size());
    }
    
    public void syncAllData() {
        syncAllUsersToElasticsearch();
        syncAllProjectsToElasticsearch();
    }

    @Transactional(readOnly = true)
    public void syncAllProjectsToElasticsearch() {
        log.info("Starting project data synchronization to Elasticsearch...");

        // findAll() 대신 findAllWithTechStacks()를 사용하여 즉시 로딩
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAllWithTechStacks();
        List<ProjectSearchDocument> projectDocs = projects.stream()
                .map(this::convertProjectToDocument)
                .collect(Collectors.toList());

        projectSearchRepository.saveAll(projectDocs);
        log.info("Synchronized {} projects to Elasticsearch", projectDocs.size());
    }
    
    public UserSearchDocument convertUserToDocument(User user) {
        log.info("Converting user to document: ID={}, Nickname={}, IsPortfolioOpen={}", 
                user.getUserId(), user.getNickname(), user.isPortfolioOpen());
        
        UserSearchDocument document = UserSearchDocument.builder()
                .id(String.valueOf(user.getUserId()))
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .techPart(user.getTechPart()) // 사용자가 설정한 기술 파트
                .techStacks(getUserTechStacks(user)) // 실제 연관관계에서 기술 스택 추출
                .introduction("안녕하세요, " + user.getNickname() + "입니다.") // 기본 소개
                .profileImage(user.getProfileImage())
                .githubUrl("https://github.com/" + user.getNickname().toLowerCase()) // 기본 GitHub URL
                .experienceRange(user.getExperienceRange()) // 경력 정보
                .isSearchable(true) // 기본값으로 검색 가능하게 설정
                .isPortfolioOpen(user.isPortfolioOpen()) // 포트폴리오 공개 여부
                .isSearchOpen(true) // 검색 허용 여부 (기본값 true)
                .createdAt(user.getCreatedAt().format(ELASTICSEARCH_DATE_FORMAT))
                .updatedAt(user.getUpdatedAt().format(ELASTICSEARCH_DATE_FORMAT))
                .build();
        
        log.info("Created document: nickname={}, techPart={}, techStacks={}", 
                document.getNickname(), document.getTechPart(), document.getTechStacks());
        return document;
    }


    private List<String> getUserTechStacks(User user) {
        // 실제 사용자의 기술 스택 연관관계에서 추출
        try {
            if (user.getUserTechStacks() != null && !user.getUserTechStacks().isEmpty()) {
                return user.getUserTechStacks().stream()
                        .map(uts -> uts.getTechStack().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // LazyInitializationException 등이 발생할 수 있으므로 로그를 남깁니다.
            log.warn("Failed to load tech stacks for user {}: {}", user.getUserId(), e.getMessage());
        }

        // 기술 스택이 없는 경우, 빈 리스트를 반환합니다.
        return new ArrayList<>();
    }

    public ProjectSearchDocument convertProjectToDocument(ProjectRecruitment project) {
        log.info("Converting project to document: ID={}, Title={}", 
                project.getProjectId(), project.getTitle());
        
        ProjectSearchDocument document = ProjectSearchDocument.builder()
                .id(String.valueOf(project.getProjectId()))
                .projectId(project.getProjectId())
                .title(project.getTitle())
                .description(project.getDescription())
                .techParts(getProjectTechParts(project))
                .techStacks(getProjectTechStacks(project))
                .status(project.getStatus().name())
                .teamLeaderId(project.getTeamLeader() != null ? project.getTeamLeader().getUserId() : null)
                .teamLeaderNickname(project.getTeamLeader() != null ? project.getTeamLeader().getNickname() : "Unknown")
                .recruitCount(project.getRecruitCount())
                .viewCount(project.getViewCount())
                .recruitDeadline(project.getRecruitDeadline() != null ? 
                    project.getRecruitDeadline().atStartOfDay().format(ELASTICSEARCH_DATE_FORMAT) : null)
                .startDate(project.getStartDate() != null ? 
                    project.getStartDate().atStartOfDay().format(ELASTICSEARCH_DATE_FORMAT) : null)
                .endDate(project.getEndDate() != null ? 
                    project.getEndDate().atStartOfDay().format(ELASTICSEARCH_DATE_FORMAT) : null)
                .createdAt(project.getCreatedAt().format(ELASTICSEARCH_DATE_FORMAT))
                .updatedAt(project.getUpdatedAt().format(ELASTICSEARCH_DATE_FORMAT))
                .build();
        
        log.info("Created project document: title={}, techParts={}, techStacks={}", 
                document.getTitle(), document.getTechParts(), document.getTechStacks());
        return document;
    }
    
    private List<String> getProjectTechParts(ProjectRecruitment project) {
        // 실제로는 ProjectTechPart 연관관계에서 추출해야 하지만, 
        // 현재는 간단하게 프로젝트 제목 기반으로 추출
        String title = project.getTitle().toLowerCase();
        List<String> techParts = new ArrayList<>();
        
        if (title.contains("backend") || title.contains("백엔드") || title.contains("서버") || title.contains("api")) {
            techParts.add("Backend");
        }
        if (title.contains("frontend") || title.contains("프론트") || title.contains("웹") || title.contains("react") || title.contains("vue")) {
            techParts.add("Frontend");
        }
        if (title.contains("mobile") || title.contains("모바일") || title.contains("앱") || title.contains("ios") || title.contains("android")) {
            techParts.add("Mobile");
        }
        if (title.contains("devops") || title.contains("인프라") || title.contains("배포") || title.contains("ci/cd")) {
            techParts.add("DevOps");
        }
        if (title.contains("ai") || title.contains("ml") || title.contains("머신러닝") || title.contains("딥러닝")) {
            techParts.add("AI/ML");
        }
        
        return techParts.isEmpty() ? List.of("Full Stack") : techParts;
    }
    
    private List<String> getProjectTechStacks(ProjectRecruitment project) {
        // 실제 프로젝트의 기술 스택 연관관계에서 추출
        try {
            if (project.getTechStacks() != null && !project.getTechStacks().isEmpty()) {
                return project.getTechStacks().stream()
                        .map(pts -> pts.getTechStack().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to load tech stacks for project {}: {}", project.getProjectId(), e.getMessage());
        }
        return List.of("Java", "Spring Boot", "React"); // 기본값
    }
}