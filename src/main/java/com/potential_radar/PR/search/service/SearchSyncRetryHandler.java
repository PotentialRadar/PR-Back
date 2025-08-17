package com.potential_radar.PR.search.service;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSyncRetryHandler {

    private final DataSyncService dataSyncService;
    private final UserSearchRepository userSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;
    private final DeadLetterService deadLetterService;

    /**
     * 재시도가 가능한 프로젝트 동기화
     */
    @Retryable(
            value = {Exception.class}, 
            maxAttempts = 3, 
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void syncProjectWithRetry(ProjectRecruitment project, String operation) {
        try {
            log.debug("Attempting to sync project {} with operation {}", project.getProjectId(), operation);
            
            switch (operation.toUpperCase()) {
                case "CREATE":
                case "UPDATE":
                    ProjectSearchDocument doc = dataSyncService.convertProjectToDocument(project);
                    projectSearchRepository.save(doc);
                    log.info("Successfully synced project {} ({})", project.getProjectId(), operation);
                    break;
                case "DELETE":
                    projectSearchRepository.deleteById(String.valueOf(project.getProjectId()));
                    log.info("Successfully deleted project {} from search index", project.getProjectId());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            log.warn("Sync attempt failed for project {} ({}): {}", 
                    project.getProjectId(), operation, e.getMessage());
            throw e; // 재시도를 위해 예외를 다시 던짐
        }
    }

    /**
     * 재시도가 가능한 사용자 동기화
     */
    @Retryable(
            value = {Exception.class}, 
            maxAttempts = 3, 
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void syncUserWithRetry(User user, String operation) {
        try {
            log.debug("Attempting to sync user {} with operation {}", user.getUserId(), operation);
            
            switch (operation.toUpperCase()) {
                case "CREATE":
                case "UPDATE":
                    UserSearchDocument doc = dataSyncService.convertUserToDocument(user);
                    userSearchRepository.save(doc);
                    log.info("Successfully synced user {} ({})", user.getUserId(), operation);
                    break;
                case "DELETE":
                    userSearchRepository.deleteById(String.valueOf(user.getUserId()));
                    log.info("Successfully deleted user {} from search index", user.getUserId());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            log.warn("Sync attempt failed for user {} ({}): {}", 
                    user.getUserId(), operation, e.getMessage());
            throw e;
        }
    }

    /**
     * 프로젝트 동기화 실패 시 복구 처리
     */
    @Recover
    public void recoverProjectSync(Exception e, ProjectRecruitment project, String operation) {
        log.error("Failed to sync project {} after 3 attempts ({}): {}", 
                project.getProjectId(), operation, e.getMessage());
        
        // Dead Letter Queue에 추가
        deadLetterService.addProjectToQueue(project, operation, e.getMessage());
        
        // 모니터링 메트릭 업데이트
        // metricsService.incrementFailedSyncCount("project", operation);
        
        // 긴급한 경우 알림 발송
        if ("CREATE".equals(operation) || "DELETE".equals(operation)) {
            // alertService.sendUrgentSyncFailureAlert("Project", project.getProjectId(), operation, e);
        }
    }

    /**
     * 사용자 동기화 실패 시 복구 처리
     */
    @Recover
    public void recoverUserSync(Exception e, User user, String operation) {
        log.error("Failed to sync user {} after 3 attempts ({}): {}", 
                user.getUserId(), operation, e.getMessage());
        
        // Dead Letter Queue에 추가
        deadLetterService.addUserToQueue(user, operation, e.getMessage());
        
        // 모니터링 메트릭 업데이트
        // metricsService.incrementFailedSyncCount("user", operation);
    }

    /**
     * ID만으로 삭제 처리하는 재시도 메서드
     */
    @Retryable(
            value = {Exception.class}, 
            maxAttempts = 3, 
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void deleteProjectWithRetry(Long projectId) {
        try {
            projectSearchRepository.deleteById(String.valueOf(projectId));
            log.info("Successfully deleted project {} from search index", projectId);
        } catch (Exception e) {
            log.warn("Delete attempt failed for project {}: {}", projectId, e.getMessage());
            throw e;
        }
    }

    /**
     * ID만으로 삭제 처리 실패 시 복구
     */
    @Recover
    public void recoverProjectDelete(Exception e, Long projectId) {
        log.error("Failed to delete project {} after 3 attempts: {}", projectId, e.getMessage());
        deadLetterService.addProjectDeleteToQueue(projectId, e.getMessage());
    }

    /**
     * 배치 동기화 재시도 처리
     */
    @Retryable(
            value = {Exception.class}, 
            maxAttempts = 2, 
            backoff = @Backoff(delay = 5000)
    )
    public void syncBatchWithRetry(Object entity, String entityType, String operation) {
        if ("PROJECT".equals(entityType) && entity instanceof ProjectRecruitment) {
            syncProjectWithRetry((ProjectRecruitment) entity, operation);
        } else if ("USER".equals(entityType) && entity instanceof User) {
            syncUserWithRetry((User) entity, operation);
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        }
    }

    /**
     * 배치 동기화 실패 복구
     */
    @Recover
    public void recoverBatchSync(Exception e, Object entity, String entityType, String operation) {
        log.error("Batch sync failed for {} {}: {}", entityType, operation, e.getMessage());
        
        if ("PROJECT".equals(entityType) && entity instanceof ProjectRecruitment) {
            recoverProjectSync(e, (ProjectRecruitment) entity, operation);
        } else if ("USER".equals(entityType) && entity instanceof User) {
            recoverUserSync(e, (User) entity, operation);
        }
    }
}