package com.potential_radar.PR.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponseDTO {
    private Long userId;
    private String userName;
    private String role; // LEADER, MEMBER
    private String techPart;
}
