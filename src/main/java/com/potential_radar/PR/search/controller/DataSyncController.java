package com.potential_radar.PR.search.controller;

import com.potential_radar.PR.search.domain.SyncStatus;
import com.potential_radar.PR.search.repository.SyncStatusRepository;
import com.potential_radar.PR.search.service.DataSyncService;
import com.potential_radar.PR.search.service.IncrementalSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/admin/sync")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DataSyncController {
    
    private final DataSyncService dataSyncService;
    private final IncrementalSyncScheduler incrementalSyncScheduler;
    private final SyncStatusRepository syncStatusRepository;
    
    
    // 전체 동기화
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> syncAll() {
        try {
            log.info("Manual full synchronization triggered");
            dataSyncService.syncAllData();
            return ResponseEntity.ok(Map.of("message", "All data synchronized successfully"));
        } catch (Exception e) {
            log.error("Full synchronization failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to sync data: " + e.getMessage()));
        }
    }
    
    // 증분 동기화 수동 실행
    @PostMapping("/incremental")
    public ResponseEntity<Map<String, String>> triggerIncrementalSync() {
        try {
            log.info("Manual incremental synchronization triggered");
            incrementalSyncScheduler.triggerAllIncrementalSync();
            return ResponseEntity.ok(Map.of("message", "Incremental synchronization completed successfully"));
        } catch (Exception e) {
            log.error("Incremental synchronization failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to perform incremental sync: " + e.getMessage()));
        }
    }
    
    // 사용자만 증분 동기화
    @PostMapping("/incremental/users")
    public ResponseEntity<Map<String, String>> syncUsersIncremental() {
        try {
            log.info("Manual user incremental synchronization triggered");
            dataSyncService.incrementalSyncUsers();
            return ResponseEntity.ok(Map.of("message", "User incremental synchronization completed successfully"));
        } catch (Exception e) {
            log.error("User incremental synchronization failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to sync users: " + e.getMessage()));
        }
    }
    
    // 프로젝트만 증분 동기화
    @PostMapping("/incremental/projects")
    public ResponseEntity<Map<String, String>> syncProjectsIncremental() {
        try {
            log.info("Manual project incremental synchronization triggered");
            dataSyncService.incrementalSyncProjects();
            return ResponseEntity.ok(Map.of("message", "Project incremental synchronization completed successfully"));
        } catch (Exception e) {
            log.error("Project incremental synchronization failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to sync projects: " + e.getMessage()));
        }
    }
    
    // 동기화 상태 조회
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            List<SyncStatus> allStatuses = syncStatusRepository.findAll();
            List<SyncStatus> failedJobs = syncStatusRepository.findFailedSyncStatuses();
            List<SyncStatus> inProgressJobs = syncStatusRepository.findInProgressSyncStatuses();
            List<Object[]> statistics = syncStatusRepository.getSyncStatusStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("allStatuses", allStatuses);
            response.put("failedJobs", failedJobs);
            response.put("inProgressJobs", inProgressJobs);
            response.put("statistics", statistics);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get sync status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get sync status: " + e.getMessage()));
        }
    }
    
    // 특정 동기화 타입의 상태 조회
    @GetMapping("/status/{syncType}")
    public ResponseEntity<SyncStatus> getSyncStatusByType(@PathVariable String syncType) {
        try {
            return syncStatusRepository.findBySyncType(syncType)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get sync status for type {}: {}", syncType, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 실패한 동기화 작업 재시도
    @PostMapping("/retry/{syncType}")
    public ResponseEntity<Map<String, String>> retrySyncJob(@PathVariable String syncType) {
        try {
            log.info("Retrying sync job for type: {}", syncType);
            
            switch (syncType.toUpperCase()) {
                case "USER_INCREMENTAL":
                    dataSyncService.incrementalSyncUsers();
                    break;
                case "PROJECT_INCREMENTAL":
                    dataSyncService.incrementalSyncProjects();
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Unknown sync type: " + syncType));
            }
            
            return ResponseEntity.ok(Map.of("message", "Sync job retried successfully for " + syncType));
        } catch (Exception e) {
            log.error("Failed to retry sync job for type {}: {}", syncType, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retry sync job: " + e.getMessage()));
        }
    }
    
    // 멈춰있는 작업들 정리
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupStuckJobs() {
        try {
            log.info("Manual cleanup of stuck jobs triggered");
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            List<SyncStatus> inProgressJobs = syncStatusRepository.findInProgressSyncStatuses();
            
            int cleanedUp = 0;
            for (SyncStatus syncStatus : inProgressJobs) {
                if (syncStatus.getUpdatedAt().isBefore(cutoffTime)) {
                    syncStatus.setStatus("FAILED");
                    syncStatus.setErrorMessage("Job timeout - manually cleaned up");
                    syncStatus.setUpdatedAt(LocalDateTime.now());
                    syncStatusRepository.save(syncStatus);
                    cleanedUp++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                    "message", "Cleanup completed", 
                    "cleanedUpJobs", String.valueOf(cleanedUp)
            ));
        } catch (Exception e) {
            log.error("Failed to cleanup stuck jobs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to cleanup stuck jobs: " + e.getMessage()));
        }
    }
}