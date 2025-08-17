package com.potential_radar.PR.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchReq {
    // 통합 검색창: 프로젝트명, 프로젝트에서 사용하는 기술 스택들과 기술 파트
    private String keyword;

    // 기술 파트 다중 선택
    private List<String> techParts;

    // 기술 스택 다중 선택
    private List<String> techStacks;

    // 정렬 기준 (deadline, createdAt, score)
    private String sortBy = "score";

    // 페이징
    private int page = 0;
    private int size = 20;
}
