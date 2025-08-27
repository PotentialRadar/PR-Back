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
import com.potential_radar.PR.search.domain.SyncStatus;
import com.potential_radar.PR.search.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

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
    private final SyncStatusRepository syncStatusRepository;
    
    @Transactional(readOnly = true)
    public void syncAllUsersToElasticsearch() {
        log.info("Starting user data synchronization to Elasticsearch...");
        
        List<User> users = userRepository.findAllWithUserProfile();
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


    @Transactional
    public void incrementalSyncUsers() {
        String syncType = "USER_INCREMENTAL";
        log.info("Starting incremental user synchronization...");
        
        SyncStatus syncStatus = getSyncStatus(syncType);
        updateSyncStatus(syncStatus, "IN_PROGRESS", null);
        
        try {
            LocalDateTime lastSyncTime = syncStatus.getLastSyncTime();
            log.info("Looking for users modified after: {}", lastSyncTime);
            
            // 마지막 동기화 이후 수정된 사용자들 조회
            List<User> modifiedUsers = userRepository.findUsersModifiedAfter(lastSyncTime);
            log.info("Found {} modified users to sync", modifiedUsers.size());
            
            if (!modifiedUsers.isEmpty()) {
                List<UserSearchDocument> userDocs = modifiedUsers.stream()
                        .map(this::convertUserToDocument)
                        .collect(Collectors.toList());
                
                userSearchRepository.saveAll(userDocs);
                log.info("Synchronized {} modified users to Elasticsearch", userDocs.size());
                
                // 동기화 상태 업데이트
                syncStatus.setLastSyncTime(LocalDateTime.now());
                syncStatus.setSyncedCount((long) userDocs.size());
                updateSyncStatus(syncStatus, "SUCCESS", null);
            } else {
                log.info("No modified users found since last sync");
                updateSyncStatus(syncStatus, "SUCCESS", "No changes detected");
            }
            
        } catch (Exception e) {
            log.error("Failed to perform incremental user sync: {}", e.getMessage(), e);
            updateSyncStatus(syncStatus, "FAILED", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void incrementalSyncProjects() {
        String syncType = "PROJECT_INCREMENTAL";
        log.info("Starting incremental project synchronization...");
        
        SyncStatus syncStatus = getSyncStatus(syncType);
        updateSyncStatus(syncStatus, "IN_PROGRESS", null);
        
        try {
            LocalDateTime lastSyncTime = syncStatus.getLastSyncTime();
            log.info("Looking for projects modified after: {}", lastSyncTime);
            
            // 1단계: 기본 프로젝트와 팀리더 정보 로딩
            List<ProjectRecruitment> modifiedProjects = projectRecruitmentRepository.findProjectsModifiedAfter(lastSyncTime);
            log.info("Found {} modified projects to sync", modifiedProjects.size());
            
            if (!modifiedProjects.isEmpty()) {
                // 2단계: 프로젝트 ID 목록 추출
                List<Long> projectIds = modifiedProjects.stream()
                        .map(ProjectRecruitment::getProjectId)
                        .collect(Collectors.toList());
                
                // 3단계: 기술스택 정보 로딩 (별도 쿼리)
                List<ProjectRecruitment> projectsWithTechStacks = projectRecruitmentRepository.findProjectsWithTechStacks(projectIds);
                Map<Long, List<String>> techStacksMap = projectsWithTechStacks.stream()
                        .collect(Collectors.toMap(
                                ProjectRecruitment::getProjectId,
                                this::getProjectTechStacks
                        ));
                
                // 4단계: 기술파트 정보 로딩 (별도 쿼리)
                List<ProjectRecruitment> projectsWithTechParts = projectRecruitmentRepository.findProjectsWithTechParts(projectIds);
                Map<Long, List<String>> techPartsMap = projectsWithTechParts.stream()
                        .collect(Collectors.toMap(
                                ProjectRecruitment::getProjectId,
                                this::getProjectTechParts
                        ));
                
                // 5단계: 검색 문서 생성 (캐시된 기술스택/기술파트 정보 사용)
                List<ProjectSearchDocument> projectDocs = modifiedProjects.stream()
                        .map(project -> convertProjectToDocumentWithCache(project, techStacksMap, techPartsMap))
                        .collect(Collectors.toList());
                
                projectSearchRepository.saveAll(projectDocs);
                log.info("Synchronized {} modified projects to Elasticsearch", projectDocs.size());
                
                // 동기화 상태 업데이트
                syncStatus.setLastSyncTime(LocalDateTime.now());
                syncStatus.setSyncedCount((long) projectDocs.size());
                updateSyncStatus(syncStatus, "SUCCESS", null);
            } else {
                log.info("No modified projects found since last sync");
                updateSyncStatus(syncStatus, "SUCCESS", "No changes detected");
            }
            
        } catch (Exception e) {
            log.error("Failed to perform incremental project sync: {}", e.getMessage(), e);
            updateSyncStatus(syncStatus, "FAILED", e.getMessage());
            throw e;
        }
    }

    private SyncStatus getSyncStatus(String syncType) {
        Optional<SyncStatus> optionalStatus = syncStatusRepository.findById(syncType);
        
        if (optionalStatus.isPresent()) {
            return optionalStatus.get();
        } else {
            // 처음 실행하는 경우 새로운 상태 생성
            SyncStatus newStatus = SyncStatus.builder()
                    .syncType(syncType)
                    .lastSyncTime(LocalDateTime.now().minusDays(1)) // 1일 전부터 동기화
                    .status("PENDING")
                    .syncedCount(0L)
                    .totalCount(0L)
                    .build();
            
            return syncStatusRepository.save(newStatus);
        }
    }

    private void updateSyncStatus(SyncStatus syncStatus, String status, String errorMessage) {
        syncStatus.setStatus(status);
        syncStatus.setErrorMessage(errorMessage);
        syncStatus.setUpdatedAt(LocalDateTime.now());
        syncStatusRepository.save(syncStatus);
        
        log.info("Updated sync status for {}: {} - {}", 
                syncStatus.getSyncType(), status, errorMessage != null ? errorMessage : "OK");
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
        String jobTitle = user.getUserProfile() != null ? user.getUserProfile().getJobTitle() : null;
        log.info("Converting user to document: ID={}, Nickname={}, JobTitle={}, IsPortfolioOpen={}", 
                user.getUserId(), user.getNickname(), jobTitle, user.isPortfolioOpen());
        
        UserSearchDocument document = UserSearchDocument.builder()
                .id(String.valueOf(user.getUserId()))
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .techPart(user.getTechPart() != null ? user.getTechPart().getName() : null) // 사용자가 설정한 기술 파트
                .techStacks(getUserTechStacks(user)) // 실제 연관관계에서 기술 스택 추출
                .introduction("안녕하세요, " + user.getNickname() + "입니다.") // 기본 소개
                .profileImage(user.getProfileImage())
                .githubUrl("https://github.com/" + user.getNickname().toLowerCase()) // 기본 GitHub URL
                .jobTitle(user.getUserProfile() != null ? user.getUserProfile().getJobTitle() : null) // 사용자 직무
                .experienceRange(user.getExperienceRange()) // 경력 정보
                .isSearchable(true) // 검색 가능 여부 (기본값 true)
                .isPortfolioOpen(user.isPortfolioOpen()) // 포트폴리오 공개 여부
                .isSearchOpen(user.isSearchOpen()) // 실제 사용자 설정 반영
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

    // 캐시된 기술스택/기술파트 정보를 사용하는 변환 메서드
    public ProjectSearchDocument convertProjectToDocumentWithCache(ProjectRecruitment project, 
                                                                  Map<Long, List<String>> techStacksMap,
                                                                  Map<Long, List<String>> techPartsMap) {
        log.debug("Converting project to document with cache: ID={}, Title={}", 
                project.getProjectId(), project.getTitle());
        
        ProjectSearchDocument document = ProjectSearchDocument.builder()
                .id(String.valueOf(project.getProjectId()))
                .projectId(project.getProjectId())
                .title(project.getTitle())
                .description(project.getDescription())
                .techParts(techPartsMap.getOrDefault(project.getProjectId(), new ArrayList<>()))
                .techStacks(techStacksMap.getOrDefault(project.getProjectId(), new ArrayList<>()))
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
        
        log.debug("Created project document with cache: title={}, techParts={}, techStacks={}", 
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