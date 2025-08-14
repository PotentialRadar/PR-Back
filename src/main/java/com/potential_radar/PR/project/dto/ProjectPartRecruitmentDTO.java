package com.potential_radar.PR.project.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectPartRecruitmentDTO {
    private String partName;     // FRONTEND / BACKEND / DEVOPS ...
    private Integer recruitCount;
}
