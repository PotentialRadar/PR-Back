package com.potential_radar.PR.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMemberResponseDTO {
    private Long id;                  // 지원 PK
    private Long userId;              // 지원자 ID
    private String userName;          // 지원자 이름 (추가로 받고 싶으면)
    private String techStack;         // 지원자 기술스택
    private String applicationMessage;// 지원자 메시지
    private String status;            // 지원 상태(APPLIED, ACCEPTED 등)
}
