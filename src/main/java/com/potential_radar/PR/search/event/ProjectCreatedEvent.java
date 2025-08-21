package com.potential_radar.PR.search.event;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ProjectCreatedEvent {
    private final ProjectRecruitment project;
    private final LocalDateTime occurredAt;

    public ProjectCreatedEvent(ProjectRecruitment project) {
        this.project = project;
        this.occurredAt = LocalDateTime.now();
    }
}