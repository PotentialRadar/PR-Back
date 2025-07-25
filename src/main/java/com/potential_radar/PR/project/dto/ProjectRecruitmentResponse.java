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
    private Integer recruitCount;      // 총 모집 인원(작성자 입력)
    private Integer appliedCount;      // 전체 지원자 수
    private Integer acceptedCount;     // 승인된 지원자 수
    private Integer remainingCount;    // 남은 모집 인원
    private List<ProjectTechStackDTO> techStacks;
}
