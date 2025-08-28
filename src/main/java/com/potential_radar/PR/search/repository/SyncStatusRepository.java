package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.domain.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatus, String> {
    
    // 특정 동기화 타입의 상태 조회
    Optional<SyncStatus> findBySyncType(String syncType);
    
    // 실패한 동기화 작업들 조회
    @Query("SELECT s FROM SyncStatus s WHERE s.status = 'FAILED' ORDER BY s.updatedAt DESC")
    List<SyncStatus> findFailedSyncStatuses();
    
    // 진행 중인 동기화 작업들 조회
    @Query("SELECT s FROM SyncStatus s WHERE s.status = 'IN_PROGRESS' ORDER BY s.updatedAt DESC")
    List<SyncStatus> findInProgressSyncStatuses();
    
    // 특정 시간 이전에 마지막으로 동기화된 작업들 조회
    @Query("SELECT s FROM SyncStatus s WHERE s.lastSyncTime < ?1 ORDER BY s.lastSyncTime ASC")
    List<SyncStatus> findStaleSync(LocalDateTime cutoffTime);
    
    // 동기화 상태별 통계
    @Query("SELECT s.status, COUNT(s) FROM SyncStatus s GROUP BY s.status")
    List<Object[]> getSyncStatusStatistics();
}