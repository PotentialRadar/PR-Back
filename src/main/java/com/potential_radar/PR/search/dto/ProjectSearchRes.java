package com.potential_radar.PR.search.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProjectSearchRes {
    
    private Long projectId;
    private String title;
    private String description;
    private List<String> techParts;
    private List<String> techStacks;
    private String status;
    private Long teamLeaderId;
    private String teamLeaderNickname;
    private Integer recruitCount;
    private Integer viewCount;
    private Integer likeCount;
    private String recruitDeadline;
    private String startDate;
    private String endDate;
    private String createdAt;
    
    // 검색 관련
    private Double matchScore;
    
    @Builder
    public ProjectSearchRes(Long projectId, String title, String description,
                           List<String> techParts, List<String> techStacks, String status,
                           Long teamLeaderId, String teamLeaderNickname, Integer recruitCount,
                           Integer viewCount, Integer likeCount, String recruitDeadline, String startDate,
                           String endDate, String createdAt, Double matchScore) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.techParts = techParts;
        this.techStacks = techStacks;
        this.status = status;
        this.teamLeaderId = teamLeaderId;
        this.teamLeaderNickname = teamLeaderNickname;
        this.recruitCount = recruitCount;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.recruitDeadline = recruitDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.matchScore = matchScore;
    }
}