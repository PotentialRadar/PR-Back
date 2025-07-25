package com.potential_radar.PR.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProjectMemberStatusUpdateRequest {
    private String status;  //승인,거절
}
