package com.potential_radar.PR.search.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedSearchRes {
    private SearchResult<UserSearchRes> users;
    private SearchResult<ProjectSearchRes> projects;
    private long totalSearchTimeMs;
}