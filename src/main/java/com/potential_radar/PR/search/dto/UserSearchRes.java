package com.potential_radar.PR.search.dto;

import com.potential_radar.PR.user.domain.ExperienceRange;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchRes {
    private Long userId;
    private String nickname;
    private String techPart;
    private List<String> techStacks;
    private String introduction;
    private String profileImage;
    private String githubUrl;
    private ExperienceRange experienceRange;
    private String createdAt;

    // 검색 관련
    private Double matchScore;
}