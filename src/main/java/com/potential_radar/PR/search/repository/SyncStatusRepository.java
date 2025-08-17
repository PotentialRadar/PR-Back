package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.domain.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatus, String> {
    
    Optional<SyncStatus> findBySyncType(String syncType);
    
    List<SyncStatus> findByStatus(String status);
    
    List<SyncStatus> findByLastSyncTimeBefore(LocalDateTime cutoff);
    
    List<SyncStatus> findBySyncTypeContainingOrderByLastSyncTimeDesc(String syncTypePattern);
}