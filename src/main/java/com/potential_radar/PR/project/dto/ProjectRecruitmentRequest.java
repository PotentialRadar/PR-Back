package com.potential_radar.PR.project.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectRecruitmentRequest {
    private String title;
    private String description;
    private LocalDate recruitDeadline;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fileUrl;
    private List<ProjectTechStackDTO> techStacks;
}
