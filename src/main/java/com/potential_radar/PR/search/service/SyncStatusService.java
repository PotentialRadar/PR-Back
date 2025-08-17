package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.domain.SyncStatus;
import com.potential_radar.PR.search.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncStatusService {
    
    private final SyncStatusRepository syncStatusRepository;
    
    /**
     * 동기화 상태 업데이트
     */
    @Transactional
    public void updateSyncStatus(String syncType, LocalDateTime syncTime, String status, String errorMessage) {
        SyncStatus syncStatus = syncStatusRepository.findBySyncType(syncType)
                .orElse(new SyncStatus(syncType));
        
        syncStatus.setLastSyncTime(syncTime);
        syncStatus.setStatus(status);
        syncStatus.setErrorMessage(errorMessage);
        
        syncStatusRepository.save(syncStatus);
        
        log.info("Updated sync status for {}: {} at {}", syncType, status, syncTime);
    }
    
    /**
     * 동기화 상태 업데이트 (카운트 포함)
     */
    @Transactional
    public void updateSyncStatus(String syncType, LocalDateTime syncTime, String status, 
                               String errorMessage, Long syncedCount, Long totalCount) {
        SyncStatus syncStatus = syncStatusRepository.findBySyncType(syncType)
                .orElse(new SyncStatus(syncType));
        
        syncStatus.setLastSyncTime(syncTime);
        syncStatus.setStatus(status);
        syncStatus.setErrorMessage(errorMessage);
        syncStatus.setSyncedCount(syncedCount);
        syncStatus.setTotalCount(totalCount);
        
        syncStatusRepository.save(syncStatus);
        
        log.info("Updated sync status for {}: {} ({}/{}) at {}", 
                syncType, status, syncedCount, totalCount, syncTime);
    }
    
    /**
     * 마지막 동기화 시간 조회
     */
    public LocalDateTime getLastSyncTime(String syncType) {
        return syncStatusRepository.findBySyncType(syncType)
                .map(SyncStatus::getLastSyncTime)
                .orElse(LocalDateTime.now().minusDays(1)); // 기본값: 1일 전
    }
    
    /**
     * 동기화 상태 조회
     */
    public Optional<SyncStatus> getSyncStatus(String syncType) {
        return syncStatusRepository.findBySyncType(syncType);
    }
    
    /**
     * 모든 동기화 상태 조회
     */
    public List<SyncStatus> getAllSyncStatuses() {
        return syncStatusRepository.findAll();
    }
    
    /**
     * 실패한 동기화 작업 조회
     */
    public List<SyncStatus> getFailedSyncs() {
        return syncStatusRepository.findByStatus("FAILED");
    }
    
    /**
     * 진행중인 동기화 작업 조회
     */
    public List<SyncStatus> getInProgressSyncs() {
        return syncStatusRepository.findByStatus("IN_PROGRESS");
    }
    
    /**
     * 특정 패턴의 동기화 상태 조회 (예: "USER"로 시작하는 모든 동기화)
     */
    public List<SyncStatus> getSyncStatusesByPattern(String pattern) {
        return syncStatusRepository.findBySyncTypeContainingOrderByLastSyncTimeDesc(pattern);
    }
    
    /**
     * 동기화 시작 표시
     */
    @Transactional
    public void markSyncStarted(String syncType) {
        updateSyncStatus(syncType, LocalDateTime.now(), "IN_PROGRESS", null);
    }
    
    /**
     * 동기화 성공 표시
     */
    @Transactional
    public void markSyncSuccess(String syncType, Long syncedCount, Long totalCount) {
        updateSyncStatus(syncType, LocalDateTime.now(), "SUCCESS", null, syncedCount, totalCount);
    }
    
    /**
     * 동기화 실패 표시
     */
    @Transactional
    public void markSyncFailed(String syncType, String errorMessage) {
        updateSyncStatus(syncType, LocalDateTime.now(), "FAILED", errorMessage);
    }
    
    /**
     * 오래된 동기화 상태 정리
     */
    @Transactional
    public void cleanupOldSyncStatuses(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        List<SyncStatus> oldStatuses = syncStatusRepository.findByLastSyncTimeBefore(cutoff);
        
        if (!oldStatuses.isEmpty()) {
            syncStatusRepository.deleteAll(oldStatuses);
            log.info("Cleaned up {} old sync status records older than {}", oldStatuses.size(), cutoff);
        }
    }
    
    /**
     * 동기화 진행률 계산
     */
    public double getSyncProgress(String syncType) {
        Optional<SyncStatus> syncStatus = getSyncStatus(syncType);
        
        if (syncStatus.isPresent() && syncStatus.get().getTotalCount() != null && syncStatus.get().getTotalCount() > 0) {
            long synced = syncStatus.get().getSyncedCount() != null ? syncStatus.get().getSyncedCount() : 0;
            long total = syncStatus.get().getTotalCount();
            return (double) synced / total * 100.0;
        }
        
        return 0.0;
    }
    
    /**
     * 동기화 건강성 체크
     */
    public boolean isSyncHealthy(String syncType, int maxHoursWithoutSync) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(maxHoursWithoutSync);
        LocalDateTime lastSync = getLastSyncTime(syncType);
        
        return lastSync.isAfter(threshold);
    }
    
    /**
     * 전체 시스템 동기화 상태 요약
     */
    public SyncHealthSummary getSyncHealthSummary() {
        List<SyncStatus> allStatuses = getAllSyncStatuses();
        
        long successCount = allStatuses.stream()
                .filter(s -> "SUCCESS".equals(s.getStatus()))
                .count();
        
        long failedCount = allStatuses.stream()
                .filter(s -> "FAILED".equals(s.getStatus()))
                .count();
        
        long inProgressCount = allStatuses.stream()
                .filter(s -> "IN_PROGRESS".equals(s.getStatus()))
                .count();
        
        return new SyncHealthSummary(successCount, failedCount, inProgressCount, allStatuses.size());
    }
    
    /**
     * 동기화 건강성 요약 클래스
     */
    public static class SyncHealthSummary {
        private final long successCount;
        private final long failedCount;
        private final long inProgressCount;
        private final long totalCount;
        
        public SyncHealthSummary(long successCount, long failedCount, long inProgressCount, long totalCount) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.inProgressCount = inProgressCount;
            this.totalCount = totalCount;
        }
        
        public long getSuccessCount() { return successCount; }
        public long getFailedCount() { return failedCount; }
        public long getInProgressCount() { return inProgressCount; }
        public long getTotalCount() { return totalCount; }
        public double getSuccessRate() { 
            return totalCount > 0 ? (double) successCount / totalCount * 100.0 : 0.0; 
        }
    }
}