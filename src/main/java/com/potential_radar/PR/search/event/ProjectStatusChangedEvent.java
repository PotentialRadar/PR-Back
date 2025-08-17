package com.potential_radar.PR.search.event;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ProjectStatusChangedEvent {
    private final ProjectRecruitment project;
    private final ProjectStatus previousStatus;
    private final ProjectStatus newStatus;
    private final LocalDateTime occurredAt;

    public ProjectStatusChangedEvent(ProjectRecruitment project, ProjectStatus previousStatus, ProjectStatus newStatus) {
        this.project = project;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = LocalDateTime.now();
    }
}