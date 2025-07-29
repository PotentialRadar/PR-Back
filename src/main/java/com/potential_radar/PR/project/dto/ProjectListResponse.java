package com.potential_radar.PR.project.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectListResponse {
    private Long projectId;
    private String title;
    private String description;
    private String status;
    private Integer recruitCount;     // 총 모집인원
    private Integer appliedCount;     // 지원자 수
    private LocalDate recruitDeadline;
    private List<String> techStacks;  // String 리스트로 변환
}