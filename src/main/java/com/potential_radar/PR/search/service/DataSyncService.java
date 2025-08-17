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

import java.time.format.DateTimeFormatter;
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
        List<ProjectSearchDocument> projectDocs = projects.stream()
                .map(this::convertProjectToDocument)
                .collect(Collectors.toList());
        
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
                .techPart("Backend") // 현재 User 엔티티에 techPart 정보가 없으므로 기본값
                .techStacks(List.of("Java", "Spring")) // 현재 User 엔티티에 techStack 정보가 없으므로 기본값
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
        
        log.info("Created document: nickname={}, isPortfolioOpen={}, isSearchOpen={}", 
                document.getNickname(), document.getIsPortfolioOpen(), document.getIsSearchOpen());
        return document;
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
        if (project.getTechStacks() != null && !project.getTechStacks().isEmpty()) {
            return project.getTechStacks().stream()
                    .map(pts -> pts.getTechStack().getName())
                    .collect(Collectors.toList());
        }
        return List.of("Spring Boot", "React"); // 기본값
    }
}