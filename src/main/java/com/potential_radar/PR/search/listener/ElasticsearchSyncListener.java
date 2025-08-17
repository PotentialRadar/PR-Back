package com.potential_radar.PR.search.listener;

import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.event.*;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.service.CacheService;
import com.potential_radar.PR.search.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchSyncListener {

    private final ProjectSearchRepository projectSearchRepository;
    private final UserSearchRepository userSearchRepository;
    private final DataSyncService dataSyncService;
    private final CacheService cacheService;

    /**
     * 프로젝트 생성 시 Elasticsearch 동기화
     */
    @Async("searchSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void onProjectCreated(ProjectCreatedEvent event) {
        try {
            log.info("Syncing newly created project {} to Elasticsearch", event.getProject().getProjectId());
            
            ProjectSearchDocument doc = dataSyncService.convertProjectToDocument(event.getProject());
            projectSearchRepository.save(doc);
            
            // 관련 캐시 무효화
            cacheService.clearProjectSearchCache();
            
            log.info("Successfully synced project {} to Elasticsearch", event.getProject().getProjectId());
        } catch (Exception e) {
            log.error("Failed to sync created project {} to Elasticsearch", 
                    event.getProject().getProjectId(), e);
            throw e; // 재시도를 위해 예외를 다시 던짐
        }
    }

    /**
     * 프로젝트 업데이트 시 Elasticsearch 동기화
     */
    @Async("searchSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        try {
            log.info("Syncing updated project {} to Elasticsearch", event.getProject().getProjectId());
            
            ProjectSearchDocument doc = dataSyncService.convertProjectToDocument(event.getProject());
            projectSearchRepository.save(doc); // Elasticsearch에서 save는 upsert
            
            // 관련 캐시 무효화
            cacheService.clearProjectSearchCache();
            
            log.info("Successfully synced updated project {} to Elasticsearch", event.getProject().getProjectId());
        } catch (Exception e) {
            log.error("Failed to sync updated project {} to Elasticsearch", 
                    event.getProject().getProjectId(), e);
            throw e;
        }
    }

    /**
     * 프로젝트 삭제 시 Elasticsearch에서 제거
     */
    @Async("searchSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void onProjectDeleted(ProjectDeletedEvent event) {
        try {
            log.info("Removing deleted project {} from Elasticsearch", event.getProjectId());
            
            projectSearchRepository.deleteById(String.valueOf(event.getProjectId()));
            
            // 관련 캐시 무효화
            cacheService.clearProjectSearchCache();
            
            log.info("Successfully removed project {} from Elasticsearch", event.getProjectId());
        } catch (Exception e) {
            log.error("Failed to remove deleted project {} from Elasticsearch", 
                    event.getProjectId(), e);
            throw e;
        }
    }

    /**
     * 프로젝트 상태 변경 시 특별 처리
     */
    @Async("searchSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectStatusChanged(ProjectStatusChangedEvent event) {
        try {
            log.info("Project {} status changed from {} to {}", 
                    event.getProject().getProjectId(), 
                    event.getPreviousStatus(), 
                    event.getNewStatus());

            // 모집 상태가 변경되면 즉시 동기화 (검색 결과에 영향)
            if (isSearchRelevantStatusChange(event)) {
                ProjectSearchDocument doc = dataSyncService.convertProjectToDocument(event.getProject());
                projectSearchRepository.save(doc);
                
                // 캐시 무효화
                cacheService.clearProjectSearchCache();
                
                log.info("Critical status change synced for project {}", event.getProject().getProjectId());
            }
        } catch (Exception e) {
            log.error("Failed to handle project status change for project {}", 
                    event.getProject().getProjectId(), e);
        }
    }

    /**
     * 사용자 업데이트 시 Elasticsearch 동기화
     */
    @Async("searchSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void onUserUpdated(UserUpdatedEvent event) {
        try {
            log.info("Syncing updated user {} to Elasticsearch", event.getUser().getUserId());
            
            UserSearchDocument doc = dataSyncService.convertUserToDocument(event.getUser());
            userSearchRepository.save(doc);
            
            // 관련 캐시 무효화
            cacheService.clearUserSearchCache();
            
            log.info("Successfully synced user {} to Elasticsearch", event.getUser().getUserId());
        } catch (Exception e) {
            log.error("Failed to sync updated user {} to Elasticsearch", 
                    event.getUser().getUserId(), e);
            throw e;
        }
    }

    /**
     * 검색에 영향을 주는 프로젝트 상태 변경인지 확인
     */
    private boolean isSearchRelevantStatusChange(ProjectStatusChangedEvent event) {
        // RECRUITING <-> 다른 상태 간의 변경은 검색 결과에 영향
        return event.getPreviousStatus().name().equals("RECRUITING") || 
               event.getNewStatus().name().equals("RECRUITING");
    }
}