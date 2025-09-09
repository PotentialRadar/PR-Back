package com.potential_radar.PR.search.dto;

import com.potential_radar.PR.user.domain.ExperienceRange;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchReq {
    // 통합 검색창: 닉네임, 유저의 기술 스택들(보유스킬)과 기술 파트(직무)
    private String keyword;

    // 기술 파트 다중 선택
    private List<String> techParts;

    // 기술 스택 다중 선택
    private List<String> techStacks;

    // 경력(ExperienceRange) 다중 선택
    private List<ExperienceRange> experienceRanges;

    // 정렬 조건 (latest, popular)
    private String sortBy = "latest";

    // 페이징
    private int page = 0;
    private int size = 20;
}
