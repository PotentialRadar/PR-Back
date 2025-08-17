package com.potential_radar.PR.search.service;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 동기화 실패한 작업들을 관리하는 Dead Letter Queue 서비스
 * 실패한 작업들을 저장하고 나중에 재처리할 수 있도록 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    // 실제 구현에서는 Redis나 Database를 사용하는 것이 좋음
    private final ConcurrentLinkedQueue<FailedSyncItem> deadLetterQueue = new ConcurrentLinkedQueue<>();

    /**
     * 실패한 프로젝트 동기화 작업 추가
     */
    public void addProjectToQueue(ProjectRecruitment project, String operation, String errorMessage) {
        FailedSyncItem item = FailedSyncItem.builder()
                .entityType("PROJECT")
                .entityId(project.getProjectId())
                .operation(operation)
                .errorMessage(errorMessage)
                .failedAt(LocalDateTime.now())
                .retryCount(0)
                .projectData(new ProjectData(project.getProjectId(), project.getTitle(), project.getStatus().name()))
                .build();

        deadLetterQueue.offer(item);
        log.warn("Added failed project sync to Dead Letter Queue: {} {} ({})", 
                operation, project.getProjectId(), errorMessage);
    }

    /**
     * 실패한 사용자 동기화 작업 추가
     */
    public void addUserToQueue(User user, String operation, String errorMessage) {
        FailedSyncItem item = FailedSyncItem.builder()
                .entityType("USER")
                .entityId(user.getUserId())
                .operation(operation)
                .errorMessage(errorMessage)
                .failedAt(LocalDateTime.now())
                .retryCount(0)
                .userData(new UserData(user.getUserId(), user.getNickname(), user.getEmail()))
                .build();

        deadLetterQueue.offer(item);
        log.warn("Added failed user sync to Dead Letter Queue: {} {} ({})", 
                operation, user.getUserId(), errorMessage);
    }

    /**
     * 실패한 프로젝트 삭제 작업 추가
     */
    public void addProjectDeleteToQueue(Long projectId, String errorMessage) {
        FailedSyncItem item = FailedSyncItem.builder()
                .entityType("PROJECT")
                .entityId(projectId)
                .operation("DELETE")
                .errorMessage(errorMessage)
                .failedAt(LocalDateTime.now())
                .retryCount(0)
                .projectData(new ProjectData(projectId, "DELETED", "UNKNOWN"))
                .build();

        deadLetterQueue.offer(item);
        log.warn("Added failed project delete to Dead Letter Queue: {} ({})", projectId, errorMessage);
    }

    /**
     * Dead Letter Queue에서 아이템 조회 (처리를 위해)
     */
    public FailedSyncItem pollFailedItem() {
        return deadLetterQueue.poll();
    }

    /**
     * Dead Letter Queue 크기 조회
     */
    public int getQueueSize() {
        return deadLetterQueue.size();
    }

    /**
     * Dead Letter Queue 비우기
     */
    public void clearQueue() {
        int size = deadLetterQueue.size();
        deadLetterQueue.clear();
        log.info("Cleared Dead Letter Queue ({} items removed)", size);
    }

    /**
     * 오래된 실패 아이템 정리
     */
    public void cleanupOldFailedItems(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        int removedCount = 0;

        // 현재 큐의 모든 아이템을 확인하고 오래된 것들 제거
        ConcurrentLinkedQueue<FailedSyncItem> tempQueue = new ConcurrentLinkedQueue<>();
        
        FailedSyncItem item;
        while ((item = deadLetterQueue.poll()) != null) {
            if (item.getFailedAt().isAfter(cutoff)) {
                tempQueue.offer(item);
            } else {
                removedCount++;
            }
        }

        // 유효한 아이템들을 다시 큐에 추가
        while ((item = tempQueue.poll()) != null) {
            deadLetterQueue.offer(item);
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} old failed sync items older than {}", removedCount, cutoff);
        }
    }

    /**
     * 실패한 동기화 아이템 데이터 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FailedSyncItem {
        private String entityType; // "PROJECT" or "USER"
        private Long entityId;
        private String operation; // "CREATE", "UPDATE", "DELETE"
        private String errorMessage;
        private LocalDateTime failedAt;
        private int retryCount;
        private ProjectData projectData;
        private UserData userData;
    }

    /**
     * 프로젝트 데이터 클래스
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProjectData {
        private Long projectId;
        private String projectName;
        private String status;
    }

    /**
     * 사용자 데이터 클래스
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UserData {
        private Long userId;
        private String nickname;
        private String email;
    }
}