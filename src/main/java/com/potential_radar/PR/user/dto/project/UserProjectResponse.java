package com.potential_radar.PR.user.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProjectResponse {
    private Long projectId;
    private String title;
    private String description;
    private String status;
    private String role; // LEADER or MEMBER
    private String techPart; // 참여한 파트
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> techStacks;
}