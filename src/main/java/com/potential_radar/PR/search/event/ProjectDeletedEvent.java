package com.potential_radar.PR.search.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ProjectDeletedEvent {
    private final Long projectId;
    private final String projectName;
    private final LocalDateTime occurredAt;

    public ProjectDeletedEvent(Long projectId, String projectName) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.occurredAt = LocalDateTime.now();
    }
}