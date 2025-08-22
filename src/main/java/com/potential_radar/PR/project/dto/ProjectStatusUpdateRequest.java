package com.potential_radar.PR.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectStatusUpdateRequest {
    @NotBlank
    private String status;
}
