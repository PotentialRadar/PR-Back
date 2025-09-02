package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.domain.SyncStatus;
import com.potential_radar.PR.search.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncrementalSyncScheduler {

    private final DataSyncService dataSyncService;
    private final SyncStatusRepository syncStatusRepository;
    
    private volatile boolean userSyncRunning = false;
    private volatile boolean projectSyncRunning = false;
    
    @Value("${app.sync.incremental.user.interval:600000}")  // 기본 10분
    private long userSyncIntervalMs;
    
    @Value("${app.sync.incremental.project.interval:600000}")  // 기본 10분
    private long projectSyncIntervalMs;
    
    @Value("${app.sync.incremental.enabled:true}")
    private boolean incrementalSyncEnabled;
    
    @Value("${app.sync.stuck-job-timeout-minutes:30}")
    private int stuckJobTimeoutMinutes;
    
    @Scheduled(fixedRateString = "${app.sync.incremental.user.interval:600000}")
    public void scheduleUserIncrementalSync() {
        if (!incrementalSyncEnabled) {
            log.debug("Incremental sync is disabled, skipping user sync");
            return;
        }
        
        if (userSyncRunning) {
            log.warn("User sync already running, skipping this execution");
            return;
        }
        
        userSyncRunning = true;
        try {
            log.info("Starting scheduled user incremental synchronization...");
            dataSyncService.incrementalSyncUsers();
            log.info("Scheduled user incremental synchronization completed");
        } catch (Exception e) {
            log.error("Scheduled user incremental synchronization failed: {}", e.getMessage(), e);
        } finally {
            userSyncRunning = false;
        }
    }
    
    @Scheduled(fixedRateString = "${app.sync.incremental.project.interval:600000}")
    public void scheduleProjectIncrementalSync() {
        if (!incrementalSyncEnabled) {
            log.debug("Incremental sync is disabled, skipping project sync");
            return;
        }
        
        if (projectSyncRunning) {
            log.warn("Project sync already running, skipping this execution");
            return;
        }
        
        projectSyncRunning = true;
        try {
            log.info("Starting scheduled project incremental synchronization...");
            dataSyncService.incrementalSyncProjects();
            log.info("Scheduled project incremental synchronization completed");
        } catch (Exception e) {
            log.error("Scheduled project incremental synchronization failed: {}", e.getMessage(), e);
        } finally {
            projectSyncRunning = false;
        }
    }
    
    // 매시간마다 실행 - 멈춰있는 동기화 작업들 정리
    @Scheduled(cron = "0 0 * * * ?") // 매시간 정각
    public void cleanupStuckSyncJobs() {
        try {
            log.info("Checking for stuck sync jobs...");
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(stuckJobTimeoutMinutes);
            List<SyncStatus> inProgressJobs = syncStatusRepository.findInProgressSyncStatuses();
            
            int cleanedUp = 0;
            for (SyncStatus syncStatus : inProgressJobs) {
                LocalDateTime updatedAt = syncStatus.getUpdatedAt();
                if (updatedAt == null || updatedAt.isBefore(cutoffTime)) {
                    log.warn("Found stuck sync job: {} - last updated: {}", 
                            syncStatus.getSyncType(), syncStatus.getUpdatedAt());
                    
                    syncStatus.setStatus("FAILED");
                    syncStatus.setErrorMessage("Job timeout - manually cleaned up");
                    syncStatus.setUpdatedAt(LocalDateTime.now());
                    syncStatusRepository.save(syncStatus);
                    cleanedUp++;
                }
            }
            
            if (cleanedUp > 0) {
                log.info("Cleaned up {} stuck sync jobs", cleanedUp);
            } else {
                log.debug("No stuck sync jobs found");
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup stuck sync jobs: {}", e.getMessage(), e);
        }
    }
    
    // 매일 자정 실행 - 전체 동기화 상태 리포트
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    public void generateDailySyncReport() {
        try {
            log.info("Generating daily sync status report...");
            
            List<Object[]> statistics = syncStatusRepository.getSyncStatusStatistics();
            StringBuilder report = new StringBuilder("=== Daily Sync Status Report ===\n");
            
            for (Object[] stat : statistics) {
                String status = (String) stat[0];
                Long count = (Long) stat[1];
                report.append(String.format("- %s: %d jobs\n", status, count));
            }
            
            List<SyncStatus> failedJobs = syncStatusRepository.findFailedSyncStatuses();
            if (!failedJobs.isEmpty()) {
                report.append("\n=== Recent Failed Jobs ===\n");
                failedJobs.stream().limit(5).forEach(job -> {
                    report.append(String.format("- %s: %s (at %s)\n", 
                            job.getSyncType(), job.getErrorMessage(), job.getUpdatedAt()));
                });
            }
            
            log.info(report.toString());
            
        } catch (Exception e) {
            log.error("Failed to generate daily sync report: {}", e.getMessage(), e);
        }
    }
    
    // 수동으로 모든 증분 동기화 실행
    public void triggerAllIncrementalSync() {
        if (!incrementalSyncEnabled) {
            log.warn("Incremental sync is disabled, cannot trigger manual sync");
            return;
        }
        
        log.info("Manually triggering all incremental synchronizations...");
        
        try {
            dataSyncService.incrementalSyncUsers();
            dataSyncService.incrementalSyncProjects();
            log.info("Manual incremental synchronization completed successfully");
        } catch (Exception e) {
            log.error("Manual incremental synchronization failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}