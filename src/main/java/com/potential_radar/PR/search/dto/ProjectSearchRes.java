package com.potential_radar.PR.search.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchRes {
    private Long projectId;
    private String projectName;
    private String description;
    private List<String> techStacks;
    private List<String> requiredTechParts;
    private String status;
    private Long ownerId;
    private String ownerNickname;
    private LocalDateTime createdAt;

    // 검색 관련
    private Double matchScore;
}