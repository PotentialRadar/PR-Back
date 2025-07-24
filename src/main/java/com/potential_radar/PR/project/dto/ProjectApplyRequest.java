package com.potential_radar.PR.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProjectApplyRequest {
    private Long userId;
    private String applicationMessage;    // 지원 메시지
    private String techStack;             // 지원자가 선택한 기술스택
}
