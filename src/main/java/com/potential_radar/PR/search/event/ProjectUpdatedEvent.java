package com.potential_radar.PR.search.event;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ProjectUpdatedEvent {
    private final ProjectRecruitment project;
    private final ProjectRecruitment previousProject;
    private final LocalDateTime occurredAt;

    public ProjectUpdatedEvent(ProjectRecruitment project, ProjectRecruitment previousProject) {
        this.project = project;
        this.previousProject = previousProject;
        this.occurredAt = LocalDateTime.now();
    }

    public ProjectUpdatedEvent(ProjectRecruitment project) {
        this.project = project;
        this.previousProject = null;
        this.occurredAt = LocalDateTime.now();
    }
}