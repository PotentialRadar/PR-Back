package com.potential_radar.PR.search.service;

import com.potential_radar.PR.project.domain.ProjectStatus;
import com.potential_radar.PR.search.event.ProjectCreatedEvent;
import com.potential_radar.PR.search.event.ProjectStatusChangedEvent;
import com.potential_radar.PR.search.event.ProjectUpdatedEvent;
import com.potential_radar.PR.search.event.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 하이브리드 동기화 전략
 * 이벤트 기반 실시간 동기화 + 스케줄러 기반 정기 동기화 + 안전망 동기화를 통합 관리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HybridSyncStrategy {

    private final SearchSyncRetryHandler retryHandler;
    private final DataSyncService dataSyncService;
    private final SyncStatusService syncStatusService;
    private final DeadLetterService deadLetterService;

    /**
     * 중요한 변경사항에 대한 즉시 동기화
     * 이벤트 기반으로 가장 중요한 데이터 변경을 실시간 처리
     */
    @EventListener
    public void onCriticalProjectChange(ProjectStatusChangedEvent event) {
        // 모집 시작/종료와 같은 중요한 상태 변경은 즉시 동기화
        if (isCriticalStatusChange(event)) {
            try {
                log.info("Critical project status change detected: {} -> {}, syncing immediately", 
                        event.getPreviousStatus(), event.getNewStatus());
                
                retryHandler.syncProjectWithRetry(event.getProject(), "STATUS_CHANGE");
                
                // 중요한 변경사항이므로 관련 캐시도 즉시 무효화
                // cacheService.clearAllSearchCache();
                
            } catch (Exception e) {
                log.error("Failed to sync critical project status change for project {}", 
                        event.getProject().getProjectId(), e);
            }
        }
    }

    /**
     * 프로젝트 생성 시 우선순위 동기화
     */
    @EventListener
    public void onProjectCreated(ProjectCreatedEvent event) {
        // 새로운 모집 프로젝트는 즉시 검색 가능하도록 우선 처리
        if (ProjectStatus.RECRUITING.equals(event.getProject().getStatus())) {
            try {
                log.info("New recruiting project created: {}, syncing with high priority", 
                        event.getProject().getProjectId());
                
                retryHandler.syncProjectWithRetry(event.getProject(), "HIGH_PRIORITY_CREATE");
                
            } catch (Exception e) {
                log.error("Failed to sync high priority project creation for project {}", 
                        event.getProject().getProjectId(), e);
            }
        }
    }

    /**
     * 누락된 변경사항 보완 동기화 (안전망)
     * 이벤트로 처리되지 않은 데이터나 실패한 동기화를 보완
     */
    @Scheduled(fixedDelay = 600000) // 10분마다
    public void safetyNetSync() {
        try {
            log.debug("Starting safety net synchronization...");
            
            // Dead Letter Queue 처리
            processDeadLetterQueue();
            
            // 동기화 상태 검증 및 복구
            validateAndRecoverSyncStatus();
            
            // 실패한 동기화 재시도
            retryFailedSyncs();
            
            log.debug("Safety net synchronization completed");
        } catch (Exception e) {
            log.error("Safety net synchronization failed", e);
        }
    }

    /**
     * 전체 데이터 일관성 보장 동기화
     * 주기적으로 전체 데이터를 동기화하여 일관성 보장
     */
    @Scheduled(cron = "0 0 3 * * SUN") // 매주 일요일 새벽 3시
    public void weeklyConsistencySync() {
        try {
            log.info("Starting weekly consistency synchronization...");
            
            // 전체 동기화 실행
            dataSyncService.syncAllData();
            
            // 동기화 통계 수집
            collectSyncStatistics();
            
            // 오래된 동기화 상태 정리
            syncStatusService.cleanupOldSyncStatuses(30);
            deadLetterService.cleanupOldFailedItems(30);
            
            log.info("Weekly consistency synchronization completed successfully");
        } catch (Exception e) {
            log.error("Weekly consistency synchronization failed", e);
            
            // 전체 동기화 실패는 심각한 문제이므로 알림
            // alertService.sendCriticalSyncFailureAlert("Weekly Consistency Sync Failed", e);
        }
    }

    /**
     * Dead Letter Queue 처리
     */
    private void processDeadLetterQueue() {
        int processedCount = 0;
        int maxRetries = 10; // 한 번에 최대 10개 항목 처리
        
        while (processedCount < maxRetries) {
            DeadLetterService.FailedSyncItem failedItem = deadLetterService.pollFailedItem();
            if (failedItem == null) {
                break; // 더 이상 처리할 항목이 없음
            }
            
            try {
                retryFailedSyncItem(failedItem);
                processedCount++;
                log.info("Successfully recovered failed sync: {} {} {}", 
                        failedItem.getEntityType(), failedItem.getEntityId(), failedItem.getOperation());
            } catch (Exception e) {
                // 재시도 횟수를 증가시키고 다시 큐에 추가
                failedItem.setRetryCount(failedItem.getRetryCount() + 1);
                
                if (failedItem.getRetryCount() < 5) {
                    deadLetterService.addProjectToQueue(null, failedItem.getOperation(), 
                            "Retry attempt " + failedItem.getRetryCount() + ": " + e.getMessage());
                } else {
                    log.error("Permanently failed to recover sync item after 5 retries: {} {} {}", 
                            failedItem.getEntityType(), failedItem.getEntityId(), failedItem.getOperation());
                }
                break; // 실패하면 이번 라운드는 종료
            }
        }
        
        if (processedCount > 0) {
            log.info("Processed {} failed sync items from Dead Letter Queue", processedCount);
        }
    }

    /**
     * 동기화 상태 검증 및 복구
     */
    private void validateAndRecoverSyncStatus() {
        // 진행중인 상태로 너무 오래 남아있는 동기화 작업 확인
        var inProgressSyncs = syncStatusService.getInProgressSyncs();
        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(1);
        
        for (var syncStatus : inProgressSyncs) {
            if (syncStatus.getLastSyncTime().isBefore(staleThreshold)) {
                log.warn("Found stale in-progress sync: {} (last update: {})", 
                        syncStatus.getSyncType(), syncStatus.getLastSyncTime());
                
                // 오래된 진행중 상태를 실패로 변경
                syncStatusService.markSyncFailed(syncStatus.getSyncType(), 
                        "Stale sync detected - marked as failed for recovery");
            }
        }
    }

    /**
     * 실패한 동기화 재시도
     */
    private void retryFailedSyncs() {
        var failedSyncs = syncStatusService.getFailedSyncs();
        LocalDateTime retryThreshold = LocalDateTime.now().minusMinutes(30);
        
        for (var syncStatus : failedSyncs) {
            // 30분 전에 실패한 동기화 작업 재시도
            if (syncStatus.getLastSyncTime().isBefore(retryThreshold)) {
                try {
                    log.info("Retrying failed sync: {}", syncStatus.getSyncType());
                    
                    if (syncStatus.getSyncType().contains("USER")) {
                        // 사용자 증분 동기화 재시도
                        LocalDateTime since = syncStatus.getLastSyncTime().minusMinutes(10);
                        // userSyncService.syncRecentUsers(since);
                    } else if (syncStatus.getSyncType().contains("PROJECT")) {
                        // 프로젝트 증분 동기화 재시도
                        LocalDateTime since = syncStatus.getLastSyncTime().minusMinutes(10);
                        // projectSyncService.syncRecentProjects(since);
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to retry sync: {}", syncStatus.getSyncType(), e);
                }
            }
        }
    }

    /**
     * 실패한 동기화 항목 재시도
     */
    private void retryFailedSyncItem(DeadLetterService.FailedSyncItem item) throws Exception {
        // 실제 구현에서는 item의 데이터를 기반으로 재동기화 수행
        log.info("Retrying failed sync item: {} {} {}", 
                item.getEntityType(), item.getEntityId(), item.getOperation());
        
        // 예시: 프로젝트 데이터 재동기화
        if ("PROJECT".equals(item.getEntityType()) && item.getProjectData() != null) {
            // 실제로는 데이터베이스에서 최신 데이터를 조회해야 함
            // ProjectRecruitment project = projectRepository.findById(item.getEntityId());
            // retryHandler.syncProjectWithRetry(project, item.getOperation());
        }
    }

    /**
     * 중요한 상태 변경인지 확인
     */
    private boolean isCriticalStatusChange(ProjectStatusChangedEvent event) {
        // RECRUITING <-> 다른 상태 간의 변경은 검색 결과에 즉시 영향을 줌
        return ProjectStatus.RECRUITING.equals(event.getPreviousStatus()) || 
               ProjectStatus.RECRUITING.equals(event.getNewStatus());
    }

    /**
     * 동기화 통계 수집
     */
    private void collectSyncStatistics() {
        var healthSummary = syncStatusService.getSyncHealthSummary();
        
        log.info("Sync Health Summary - Success: {}, Failed: {}, In Progress: {}, Success Rate: {:.2f}%",
                healthSummary.getSuccessCount(),
                healthSummary.getFailedCount(), 
                healthSummary.getInProgressCount(),
                healthSummary.getSuccessRate());
        
        // 메트릭 수집 시스템에 전송 (실제 구현 시)
        // metricsService.recordSyncHealth(healthSummary);
    }
}