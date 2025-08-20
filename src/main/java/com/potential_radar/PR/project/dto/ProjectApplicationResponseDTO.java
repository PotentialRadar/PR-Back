package com.potential_radar.PR.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectApplicationResponseDTO {
    private Long id;                  // 지원 PK
    private Long userId;              // 지원자 ID
    private String userName;          // 지원자 이름
    private String techPart;         // 지원자 기술 파트
    private String applicationMessage;// 지원자 메시지
    private String status;            // 지원 상태(APPLIED, ACCEPTED 등)

    // 프로젝트 정보 추가
    private Long projectId;
    private String projectTitle;
    private String projectStatus;
}
