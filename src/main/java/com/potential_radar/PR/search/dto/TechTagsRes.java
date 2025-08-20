package com.potential_radar.PR.search.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechTagsRes {
    private List<String> techParts;
    private List<PopularTechStack> popularTechStacks;
    
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PopularTechStack {
        private String name;
        private long count;
    }
}