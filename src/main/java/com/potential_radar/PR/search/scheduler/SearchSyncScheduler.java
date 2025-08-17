package com.potential_radar.PR.search.scheduler;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.search.document.ProjectSearchDocument;
import com.potential_radar.PR.search.document.UserSearchDocument;
import com.potential_radar.PR.search.repository.ProjectSearchRepository;
import com.potential_radar.PR.search.repository.UserSearchRepository;
import com.potential_radar.PR.search.service.DataSyncService;
import com.potential_radar.PR.search.service.SyncStatusService;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchSyncScheduler {

    private final DataSyncService dataSyncService;
    private final SyncStatusService syncStatusService;
    private final UserRepository userRepository;
    private final ProjectRecruitmentRepository projectRepository;
    private final UserSearchRepository userSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;

    /**
     * 5분마다 증분 동기화 실행
     * 최근 변경된 데이터만 동기화하여 성능 최적화
     */
    @Scheduled(fixedDelay = 300000) // 5분 = 300,000ms
    public void syncRecentChanges() {
        log.info("Starting incremental synchronization...");
        
        try {
            LocalDateTime lastUserSync = syncStatusService.getLastSyncTime("USER_INCREMENTAL");
            LocalDateTime lastProjectSync = syncStatusService.getLastSyncTime("PROJECT_INCREMENTAL");
            LocalDateTime now = LocalDateTime.now();

            // 사용자 증분 동기화
            syncRecentUsers(lastUserSync, now);
            
            // 프로젝트 증분 동기화
            syncRecentProjects(lastProjectSync, now);

            log.info("Incremental synchronization completed successfully");
        } catch (Exception e) {
            log.error("Incremental synchronization failed", e);
            syncStatusService.updateSyncStatus("INCREMENTAL", LocalDateTime.now(), "FAILED", e.getMessage());
        }
    }

    /**
     * 매일 새벽 2시 전체 동기화 실행
     * 데이터 일관성 보장을 위한 안전망
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullSync() {
        log.info("Starting full synchronization...");
        
        try {
            syncStatusService.updateSyncStatus("FULL", LocalDateTime.now(), "IN_PROGRESS", null);
            
            dataSyncService.syncAllData();
            
            syncStatusService.updateSyncStatus("FULL", LocalDateTime.now(), "SUCCESS", null);
            log.info("Full synchronization completed successfully");
        } catch (Exception e) {
            log.error("Full synchronization failed", e);
            syncStatusService.updateSyncStatus("FULL", LocalDateTime.now(), "FAILED", e.getMessage());
            
            // 실패 알림 (실제 구현 시 알림 서비스 연동)
            // alertService.sendSyncFailureAlert("Full Sync Failed", e);
        }
    }

    /**
     * 매주 일요일 새벽 3시 인덱스 최적화
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void weeklyOptimization() {
        log.info("Starting weekly Elasticsearch optimization...");
        
        try {
            // Elasticsearch 인덱스 최적화 (실제 구현 시 ElasticsearchOperations 사용)
            optimizeElasticsearchIndices();
            
            // 오래된 검색 히스토리 정리 (30일 이상)
            cleanupOldSearchHistory();
            
            log.info("Weekly optimization completed successfully");
        } catch (Exception e) {
            log.error("Weekly optimization failed", e);
        }
    }

    /**
     * 최근 변경된 사용자 데이터 동기화
     */
    @Transactional(readOnly = true)
    private void syncRecentUsers(LocalDateTime since, LocalDateTime now) {
        try {
            List<User> recentUsers = userRepository.findByUpdatedAtAfter(since);
            
            if (!recentUsers.isEmpty()) {
                List<UserSearchDocument> docs = recentUsers.stream()
                        .map(dataSyncService::convertUserToDocument)
                        .collect(Collectors.toList());
                
                userSearchRepository.saveAll(docs);
                log.info("Synced {} recent users (since {})", docs.size(), since);
            }
            
            syncStatusService.updateSyncStatus("USER_INCREMENTAL", now, "SUCCESS", null);
        } catch (Exception e) {
            log.error("Failed to sync recent users", e);
            syncStatusService.updateSyncStatus("USER_INCREMENTAL", now, "FAILED", e.getMessage());
            throw e;
        }
    }

    /**
     * 최근 변경된 프로젝트 데이터 동기화
     */
    @Transactional(readOnly = true)
    private void syncRecentProjects(LocalDateTime since, LocalDateTime now) {
        try {
            List<ProjectRecruitment> recentProjects = projectRepository.findByUpdatedAtAfter(since);
            
            if (!recentProjects.isEmpty()) {
                List<ProjectSearchDocument> docs = recentProjects.stream()
                        .map(dataSyncService::convertProjectToDocument)
                        .collect(Collectors.toList());
                
                projectSearchRepository.saveAll(docs);
                log.info("Synced {} recent projects (since {})", docs.size(), since);
            }
            
            syncStatusService.updateSyncStatus("PROJECT_INCREMENTAL", now, "SUCCESS", null);
        } catch (Exception e) {
            log.error("Failed to sync recent projects", e);
            syncStatusService.updateSyncStatus("PROJECT_INCREMENTAL", now, "FAILED", e.getMessage());
            throw e;
        }
    }

    /**
     * Elasticsearch 인덱스 최적화
     */
    private void optimizeElasticsearchIndices() {
        // 실제 구현 시 ElasticsearchOperations를 사용하여 인덱스 최적화
        log.info("Optimizing Elasticsearch indices (placeholder)");
        
        // 예시:
        // elasticsearchOperations.indexOps(UserSearchDocument.class).optimize();
        // elasticsearchOperations.indexOps(ProjectSearchDocument.class).optimize();
    }

    /**
     * 오래된 검색 히스토리 정리
     */
    private void cleanupOldSearchHistory() {
        // 30일 이상 된 검색 히스토리 삭제
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        log.info("Cleaning up search history older than {}", cutoffDate);
        
        // 실제 구현 시 SearchHistoryRepository에서 삭제
        // searchHistoryRepository.deleteBySearchTimeBefore(cutoffDate);
    }

    /**
     * 데이터 변경 빈도에 따른 적응형 동기화
     * 변경이 많을 때는 더 자주, 적을 때는 덜 자주 동기화
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 체크
    public void adaptiveSync() {
        try {
            SyncMetrics metrics = getSyncMetrics();
            
            if (metrics.getChangeRate() > 100) { // 분당 100개 이상 변경
                // 긴급 동기화 (1분간 변경사항)
                syncUrgentChanges(LocalDateTime.now().minusMinutes(1));
                log.info("Urgent sync executed due to high change rate: {}", metrics.getChangeRate());
            } else if (metrics.getChangeRate() > 50) { // 분당 50개 이상 변경
                // 빠른 동기화 (3분간 변경사항)
                syncRecentChanges(LocalDateTime.now().minusMinutes(3));
                log.info("Fast sync executed due to moderate change rate: {}", metrics.getChangeRate());
            }
            // 변경률이 낮으면 정기 동기화에 의존
        } catch (Exception e) {
            log.warn("Adaptive sync check failed", e);
        }
    }

    /**
     * 동기화 메트릭 조회
     */
    private SyncMetrics getSyncMetrics() {
        // 실제 구현 시 최근 변경 통계 조회
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        
        long userChanges = userRepository.countByUpdatedAtAfter(oneMinuteAgo);
        long projectChanges = projectRepository.countByUpdatedAtAfter(oneMinuteAgo);
        
        return new SyncMetrics(userChanges + projectChanges);
    }

    /**
     * 긴급 변경사항 동기화
     */
    private void syncUrgentChanges(LocalDateTime since) {
        syncRecentUsers(since, LocalDateTime.now());
        syncRecentProjects(since, LocalDateTime.now());
    }

    /**
     * 빠른 변경사항 동기화
     */
    private void syncRecentChanges(LocalDateTime since) {
        syncRecentUsers(since, LocalDateTime.now());
        syncRecentProjects(since, LocalDateTime.now());
    }

    /**
     * 동기화 메트릭 데이터 클래스
     */
    private static class SyncMetrics {
        private final long changeRate;

        public SyncMetrics(long changeRate) {
            this.changeRate = changeRate;
        }

        public long getChangeRate() {
            return changeRate;
        }
    }
}