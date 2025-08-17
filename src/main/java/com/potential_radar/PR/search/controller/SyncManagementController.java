package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.search.service.DataSyncService;
import com.potential_radar.PR.search.service.SyncStatusService;
import com.potential_radar.PR.search.service.DeadLetterService;
import com.potential_radar.PR.search.domain.SyncStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 동기화 관리 API 컨트롤러
 * 동기화 상태 모니터링 및 수동 제어 기능 제공
 */
@RestController
@RequestMapping("/api/search/admin/sync-management")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SyncManagementController {

    private final SyncStatusService syncStatusService;
    private final DeadLetterService deadLetterService;
    private final DataSyncService dataSyncService;

    /**
     * 전체 동기화 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            List<SyncStatus> allStatuses = syncStatusService.getAllSyncStatuses();
            SyncStatusService.SyncHealthSummary healthSummary = syncStatusService.getSyncHealthSummary();
            
            return ResponseEntity.ok(Map.of(
                "syncStatuses", allStatuses,
                "healthSummary", Map.of(
                    "successCount", healthSummary.getSuccessCount(),
                    "failedCount", healthSummary.getFailedCount(),
                    "inProgressCount", healthSummary.getInProgressCount(),
                    "totalCount", healthSummary.getTotalCount(),
                    "successRate", healthSummary.getSuccessRate()
                ),
                "deadLetterQueueSize", deadLetterService.getQueueSize()
            ));
        } catch (Exception e) {
            log.error("Failed to get sync status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get sync status: " + e.getMessage()));
        }
    }

    /**
     * 특정 동기화 타입 상태 조회
     */
    @GetMapping("/status/{syncType}")
    public ResponseEntity<Map<String, Object>> getSyncStatusByType(@PathVariable String syncType) {
        try {
            var syncStatus = syncStatusService.getSyncStatus(syncType);
            if (syncStatus.isPresent()) {
                double progress = syncStatusService.getSyncProgress(syncType);
                boolean isHealthy = syncStatusService.isSyncHealthy(syncType, 24); // 24시간 기준
                
                return ResponseEntity.ok(Map.of(
                    "syncStatus", syncStatus.get(),
                    "progress", progress,
                    "isHealthy", isHealthy
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to get sync status for type: {}", syncType, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get sync status: " + e.getMessage()));
        }
    }

    /**
     * 실패한 동기화 작업 목록 조회
     */
    @GetMapping("/failed")
    public ResponseEntity<List<SyncStatus>> getFailedSyncs() {
        try {
            List<SyncStatus> failedSyncs = syncStatusService.getFailedSyncs();
            return ResponseEntity.ok(failedSyncs);
        } catch (Exception e) {
            log.error("Failed to get failed syncs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 진행중인 동기화 작업 목록 조회
     */
    @GetMapping("/in-progress")
    public ResponseEntity<List<SyncStatus>> getInProgressSyncs() {
        try {
            List<SyncStatus> inProgressSyncs = syncStatusService.getInProgressSyncs();
            return ResponseEntity.ok(inProgressSyncs);
        } catch (Exception e) {
            log.error("Failed to get in-progress syncs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Dead Letter Queue 상태 조회
     */
    @GetMapping("/dead-letter-queue")
    public ResponseEntity<Map<String, Object>> getDeadLetterQueueStatus() {
        try {
            int queueSize = deadLetterService.getQueueSize();
            
            return ResponseEntity.ok(Map.of(
                "queueSize", queueSize,
                "status", queueSize > 0 ? "HAS_FAILED_ITEMS" : "HEALTHY"
            ));
        } catch (Exception e) {
            log.error("Failed to get dead letter queue status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get queue status: " + e.getMessage()));
        }
    }

    /**
     * Dead Letter Queue 비우기
     */
    @DeleteMapping("/dead-letter-queue")
    public ResponseEntity<Map<String, String>> clearDeadLetterQueue() {
        try {
            int previousSize = deadLetterService.getQueueSize();
            deadLetterService.clearQueue();
            
            return ResponseEntity.ok(Map.of(
                "message", "Dead Letter Queue cleared successfully",
                "clearedItems", String.valueOf(previousSize)
            ));
        } catch (Exception e) {
            log.error("Failed to clear dead letter queue", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to clear queue: " + e.getMessage()));
        }
    }

    /**
     * 수동 전체 동기화 실행
     */
    @PostMapping("/manual/full")
    public ResponseEntity<Map<String, String>> triggerManualFullSync() {
        try {
            log.info("Manual full sync triggered by admin");
            syncStatusService.markSyncStarted("MANUAL_FULL");
            
            dataSyncService.syncAllData();
            
            syncStatusService.markSyncSuccess("MANUAL_FULL", null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Manual full synchronization completed successfully"
            ));
        } catch (Exception e) {
            log.error("Manual full sync failed", e);
            syncStatusService.markSyncFailed("MANUAL_FULL", e.getMessage());
            
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Manual full sync failed: " + e.getMessage()));
        }
    }

    /**
     * 수동 사용자 동기화 실행
     */
    @PostMapping("/manual/users")
    public ResponseEntity<Map<String, String>> triggerManualUserSync() {
        try {
            log.info("Manual user sync triggered by admin");
            syncStatusService.markSyncStarted("MANUAL_USER");
            
            dataSyncService.syncAllUsersToElasticsearch();
            
            syncStatusService.markSyncSuccess("MANUAL_USER", null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Manual user synchronization completed successfully"
            ));
        } catch (Exception e) {
            log.error("Manual user sync failed", e);
            syncStatusService.markSyncFailed("MANUAL_USER", e.getMessage());
            
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Manual user sync failed: " + e.getMessage()));
        }
    }

    /**
     * 수동 프로젝트 동기화 실행
     */
    @PostMapping("/manual/projects")
    public ResponseEntity<Map<String, String>> triggerManualProjectSync() {
        try {
            log.info("Manual project sync triggered by admin");
            syncStatusService.markSyncStarted("MANUAL_PROJECT");
            
            dataSyncService.syncAllProjectsToElasticsearch();
            
            syncStatusService.markSyncSuccess("MANUAL_PROJECT", null, null);
            
            return ResponseEntity.ok(Map.of(
                "message", "Manual project synchronization completed successfully"
            ));
        } catch (Exception e) {
            log.error("Manual project sync failed", e);
            syncStatusService.markSyncFailed("MANUAL_PROJECT", e.getMessage());
            
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Manual project sync failed: " + e.getMessage()));
        }
    }

    /**
     * 동기화 상태 리셋 (관리자용)
     */
    @PostMapping("/reset/{syncType}")
    public ResponseEntity<Map<String, String>> resetSyncStatus(@PathVariable String syncType) {
        try {
            log.info("Resetting sync status for: {}", syncType);
            syncStatusService.updateSyncStatus(syncType, 
                    java.time.LocalDateTime.now().minusDays(1), 
                    "PENDING", 
                    "Manually reset by admin");
            
            return ResponseEntity.ok(Map.of(
                "message", "Sync status reset successfully for " + syncType
            ));
        } catch (Exception e) {
            log.error("Failed to reset sync status for: {}", syncType, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reset sync status: " + e.getMessage()));
        }
    }

    /**
     * 시스템 동기화 건강성 체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkSyncHealth() {
        try {
            SyncStatusService.SyncHealthSummary healthSummary = syncStatusService.getSyncHealthSummary();
            
            boolean userSyncHealthy = syncStatusService.isSyncHealthy("USER_INCREMENTAL", 2);
            boolean projectSyncHealthy = syncStatusService.isSyncHealthy("PROJECT_INCREMENTAL", 2);
            boolean overallHealthy = userSyncHealthy && projectSyncHealthy && 
                                   healthSummary.getSuccessRate() > 80.0;
            
            return ResponseEntity.ok(Map.of(
                "overallHealthy", overallHealthy,
                "userSyncHealthy", userSyncHealthy,
                "projectSyncHealthy", projectSyncHealthy,
                "successRate", healthSummary.getSuccessRate(),
                "deadLetterQueueSize", deadLetterService.getQueueSize(),
                "healthStatus", overallHealthy ? "HEALTHY" : "UNHEALTHY"
            ));
        } catch (Exception e) {
            log.error("Failed to check sync health", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to check sync health: " + e.getMessage()));
        }
    }
}