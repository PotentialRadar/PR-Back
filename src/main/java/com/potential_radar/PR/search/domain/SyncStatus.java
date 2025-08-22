package com.potential_radar.PR.search.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncStatus {
    
    @Id
    @Column(name = "sync_type")
    private String syncType; // "USER_INCREMENTAL", "PROJECT_INCREMENTAL", "FULL", etc.
    
    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
    
    @Column(name = "last_synced_id")
    private Long lastSyncedId;
    
    @Column(name = "status")
    private String status; // "SUCCESS", "FAILED", "IN_PROGRESS"
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "synced_count")
    private Long syncedCount;
    
    @Column(name = "total_count")
    private Long totalCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public SyncStatus(String syncType) {
        this.syncType = syncType;
        this.lastSyncTime = LocalDateTime.now().minusDays(1); // 기본값: 1일 전
        this.status = "PENDING";
    }
}