package com.potential_radar.PR.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectRecruitmentRequest {
    private String title;
    private String description;
    private LocalDate recruitDeadline;
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private String status;
    @Valid
    private List<ProjectTechStackDTO> techStacks;
    @Valid
    private List<ProjectPartRecruitmentDTO> recruitmentParts;
    private Integer recruitCount;   //총 모집인원
//    private List<ProjectAttachmentDto> attachments;
}
