package com.potential_radar.PR.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchReq {
    // 사용자 닉네임 검색
    private String nickname;

    // 기술 파트 필터링
    private String techPart;

    // 기술 스택 검색 (부분 일치)
    private List<String> techStacks;

    // 페이징
    private int page = 0;
    private int size = 20;
}
