package com.potential_radar.PR.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ProjectSearchReq {
    // 프로젝트명 키워드 검색
    private String projectName;

    // 기술 스택 검색 (부분 일치)
    private List<String> techStacks;

    // 구하는 기술 파트 필터링
    private List<String> requiredTechParts;

    // 페이징
    private int page = 0;
    private int size = 20;
}
