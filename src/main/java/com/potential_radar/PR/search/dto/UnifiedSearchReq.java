package com.potential_radar.PR.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedSearchReq {
    // 통합 검색 키워드
    private String keyword;
    
    // 검색 타입 (user, project, all)
    @Builder.Default
    private String searchType = "all";
    
    // 기술 스택 필터
    private List<String> techStacks;
    
    // 기술 파트 필터
    private String techPart;
    
    // 구하는 기술 파트 필터 (프로젝트용)
    private List<String> requiredTechParts;
    
    // 페이징
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}