package com.potential_radar.PR.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTechStackDTO {
    @NotBlank
    private String techStackName;
    @NotNull
    @Min(0)
    private Integer recruitCount;
}
