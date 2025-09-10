package com.potential_radar.PR.search.event;

import com.potential_radar.PR.search.service.SmartCacheService;
import com.potential_radar.PR.search.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {
    
    private final SmartCacheService smartCacheService;
    private final DataSyncService dataSyncService;
    
    @EventListener({ProjectCreatedEvent.class, ProjectUpdatedEvent.class})
    @Async
    public void handleProjectDataChange(Object event) {
        log.info("Project data changed, invalidating search result caches only");
        
        // 검색 결과 캐시만 무효화 (인기 검색어/기술스택은 보존)
        smartCacheService.evictProjectSearchCache();
        
        // Elasticsearch 동기화
        try {
            if (event instanceof ProjectCreatedEvent) {
                ProjectCreatedEvent createdEvent = (ProjectCreatedEvent) event;
                dataSyncService.syncSingleProject(createdEvent.getProject().getProjectId());
                log.info("Project created, synced to Elasticsearch: {}", createdEvent.getProject().getProjectId());
            } else if (event instanceof ProjectUpdatedEvent) {
                ProjectUpdatedEvent updatedEvent = (ProjectUpdatedEvent) event;
                dataSyncService.syncSingleProject(updatedEvent.getProject().getProjectId());
                log.info("Project updated, synced to Elasticsearch: {}", updatedEvent.getProject().getProjectId());
            }
        } catch (Exception e) {
            log.error("Failed to sync project to Elasticsearch", e);
        }
    }
    
    @EventListener(ProjectDeletedEvent.class)
    @Async
    public void handleProjectDelete(ProjectDeletedEvent event) {
        log.info("Project deleted, removing from Elasticsearch and invalidating search result caches");
        
        // 검색 결과 캐시만 무효화 (인기 검색어/기술스택은 보존)
        smartCacheService.evictProjectSearchCache();
        
        // Elasticsearch에서 삭제
        try {
            dataSyncService.deleteProjectFromElasticsearch(event.getProjectId());
            log.info("Project deleted from Elasticsearch: {}", event.getProjectId());
        } catch (Exception e) {
            log.error("Failed to delete project from Elasticsearch", e);
        }
    }
    
    @EventListener(UserUpdatedEvent.class)
    @Async 
    public void handleUserDataChange(UserUpdatedEvent event) {
        log.info("User data changed, invalidating user search result caches only");
        
        // 사용자 검색 결과 캐시만 무효화 (인기 검색어/기술스택은 보존)
        smartCacheService.evictUserSearchCache();
        
        // Elasticsearch 동기화
        try {
            dataSyncService.syncSingleUser(event.getUser().getUserId());
            log.info("User updated, synced to Elasticsearch: {}", event.getUser().getUserId());
        } catch (Exception e) {
            log.error("Failed to sync user to Elasticsearch", e);
        }
    }
}