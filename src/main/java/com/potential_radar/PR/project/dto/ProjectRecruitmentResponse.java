package com.potential_radar.PR.project.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectRecruitmentResponse {
    private Long projectId;
    private String title;
    private String description;
    private LocalDate recruitDeadline;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fileUrl;
    private String status;
    private Integer viewCount;
    private List<ProjectTechStackDTO> techStacks;
}
