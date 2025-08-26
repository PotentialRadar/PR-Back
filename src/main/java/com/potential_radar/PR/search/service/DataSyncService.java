package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectTechPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void syncAllData() {
        syncAllUsersToElasticsearch();
        syncAllProjectsToElasticsearch();
    }

    @Transactional(readOnly = true)
    public void syncAllProjectsToElasticsearch() {
        log.info("Starting project data synchronization to Elasticsearch...");

        // MultipleBagFetchException을 피하기 위해 기본 조회 사용
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAll();
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
                .techPart(user.getTechPart() != null ? user.getTechPart().getName() : null) // 사용자가 설정한 기술 파트
                .techStacks(getUserTechStacks(user)) // 실제 연관관계에서 기술 스택 추출
                .introduction("안녕하세요, " + user.getNickname() + "입니다.") // 기본 소개
                .profileImage(user.getProfileImage())
                .githubUrl("https://github.com/" + user.getNickname().toLowerCase()) // 기본 GitHub URL
                .experienceRange(user.getExperienceRange()) // 경력 정보
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
                        .map(uts -> uts.getStack().getName())
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
        // 실제 ProjectTechPart 연관관계에서 추출
        try {
            // Lazy Loading 강제 실행
            List<ProjectTechPart> techParts = project.getTechParts();
            if (techParts != null && !techParts.isEmpty()) {
                // 실제 데이터 접근으로 Lazy Loading 트리거
                return techParts.stream()
                        .map(ptp -> {
                            // TechPart도 Lazy Loading일 수 있으므로 안전하게 처리
                            try {
                                return ptp.getTechPart().getName();
                            } catch (Exception e) {
                                log.warn("Failed to access techPart name for project {}: {}", 
                                        project.getProjectId(), e.getMessage());
                                return null;
                            }
                        })
                        .filter(name -> name != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to load tech parts for project {}: {}", project.getProjectId(), e.getMessage());
        }
        
        // 연관관계에서 가져올 수 없는 경우 빈 리스트 반환
        return new ArrayList<>();
    }
    
    private List<String> getProjectTechStacks(ProjectRecruitment project) {
        // 실제 프로젝트의 기술 스택 연관관계에서 추출
        try {
            if (project.getTechStacks() != null && !project.getTechStacks().isEmpty()) {
                return project.getTechStacks().stream()
                        .map(pts -> pts.getTechStack().getName())
                        .filter(name -> name != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to load tech stacks for project {}: {}", project.getProjectId(), e.getMessage());
        }
        return new ArrayList<>(); // 빈 리스트 반환
    }
}