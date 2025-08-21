package com.potential_radar.PR.search.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class ProjectSearchReq {
    
    private String keyword;              // 통합 키워드 검색 (제목, 설명, 기술파트, 기술스택)
    private List<String> techParts;      // 기술 파트 필터
    private List<String> techStacks;     // 기술 스택 필터
    private List<String> statuses;       // 프로젝트 상태 필터
    private int page = 0;                // 페이지 번호
    private int size = 20;               // 페이지 크기
    
    @Builder
    public ProjectSearchReq(String keyword, List<String> techParts, List<String> techStacks,
                           List<String> statuses, int page, int size) {
        this.keyword = keyword;
        this.techParts = techParts;
        this.techStacks = techStacks;
        this.statuses = statuses;
        this.page = page;
        this.size = size;
    }
}