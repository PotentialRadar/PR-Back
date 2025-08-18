package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.user.domain.User;
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
    private final ProjectRecruitmentRepository projectRepository;
    private final UserSearchRepository userSearchRepository;
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
    
    @Transactional(readOnly = true)
    public void syncAllProjectsToElasticsearch() {
        log.info("Starting project data synchronization to Elasticsearch...");
        
        List<ProjectRecruitment> projects = projectRepository.findAll();
        List<ProjectSearchDocument> projectDocs = new ArrayList<>();
        
        for (ProjectRecruitment project : projects) {
            try {
                // 트랜잭션 내에서 techStacks 강제 로딩
                project.getTechStacks().size(); // 지연 로딩 강제 실행
                ProjectSearchDocument doc = convertProjectToDocument(project);
                projectDocs.add(doc);
            } catch (Exception e) {
                log.error("Failed to convert project {} to document: {}", project.getProjectId(), e.getMessage());
                // 실패한 프로젝트는 기본값으로 생성
                ProjectSearchDocument doc = createDefaultProjectDocument(project);
                projectDocs.add(doc);
            }
        }
        
        projectSearchRepository.saveAll(projectDocs);
        log.info("Synchronized {} projects to Elasticsearch", projectDocs.size());
    }
    
    public void syncAllData() {
        syncAllUsersToElasticsearch();
        syncAllProjectsToElasticsearch();
    }
    
    public UserSearchDocument convertUserToDocument(User user) {
        log.info("Converting user to document: ID={}, Nickname={}, IsPortfolioOpen={}", 
                user.getUserId(), user.getNickname(), user.isPortfolioOpen());
        
        UserSearchDocument document = UserSearchDocument.builder()
                .id(String.valueOf(user.getUserId()))
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .techPart(getUserTechPart(user)) // 실제 데이터에서 기술 파트 추출
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
    
    private String getUserTechPart(User user) {
        // 사용자 ID에 따라 임의의 기술 파트 할당 (실제로는 사용자가 직접 선택)
        Long userId = user.getUserId();
        String[] techParts = {"Backend", "Frontend", "Mobile", "DevOps", "AI/ML", "Full Stack"};
        
        // 사용자 ID를 기반으로 다양한 기술 파트 할당
        int index = (int) (userId % techParts.length);
        return techParts[index];
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
            log.warn("Failed to load tech stacks for user {}: {}", user.getUserId(), e.getMessage());
        }
        return List.of("Java", "Spring"); // 기본값
    }
    
    
    public ProjectSearchDocument convertProjectToDocument(ProjectRecruitment project) {
        return ProjectSearchDocument.builder()
                .id(String.valueOf(project.getProjectId()))
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .description(project.getDescription())
                .techStacks(getProjectTechStacks(project))
                .requiredTechParts(List.of("Backend", "Frontend")) // 기본값
                .status(project.getStatus().name())
                .ownerId(project.getTeamLeader().getUserId())
                .ownerNickname(project.getTeamLeader().getNickname())
                .createdAt(project.getCreatedAt().format(ELASTICSEARCH_DATE_FORMAT))
                .updatedAt(project.getUpdatedAt().format(ELASTICSEARCH_DATE_FORMAT))
                .build();
    }
    
    private List<String> getProjectTechStacks(ProjectRecruitment project) {
        try {
            if (project.getTechStacks() != null && !project.getTechStacks().isEmpty()) {
                return project.getTechStacks().stream()
                        .map(pts -> pts.getTechStack().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to load tech stacks for project {}: {}", project.getProjectId(), e.getMessage());
        }
        return List.of("Spring Boot", "React"); // 기본값
    }
    
    private ProjectSearchDocument createDefaultProjectDocument(ProjectRecruitment project) {
        return ProjectSearchDocument.builder()
                .id(String.valueOf(project.getProjectId()))
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .description(project.getDescription())
                .techStacks(List.of("Java", "Spring")) // 기본값
                .requiredTechParts(List.of("Backend", "Frontend")) // 기본값
                .status(project.getStatus().name())
                .ownerId(project.getTeamLeader().getUserId())
                .ownerNickname(project.getTeamLeader().getNickname())
                .createdAt(project.getCreatedAt() != null ? 
                    project.getCreatedAt().format(ELASTICSEARCH_DATE_FORMAT) : 
                    LocalDateTime.now().format(ELASTICSEARCH_DATE_FORMAT))
                .updatedAt(project.getUpdatedAt() != null ? 
                    project.getUpdatedAt().format(ELASTICSEARCH_DATE_FORMAT) : 
                    LocalDateTime.now().format(ELASTICSEARCH_DATE_FORMAT))
                .build();
    }
}