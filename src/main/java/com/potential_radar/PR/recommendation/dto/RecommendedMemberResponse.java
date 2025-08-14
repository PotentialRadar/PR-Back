package com.potential_radar.PR.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// [팀장 대상] 특정 프로젝트에 적합한 멤버 목록을 추천
public class RecommendedMemberResponse {

    private Long userId;
    private String nickname;
    private String email;
    private String profileImage;

    private String position;         // BACKEND, FRONTEND 등
    private List<String> techStacks; // 기술 스택 이름들
//  private String experienceRange;  // 신입/주니어/미들/시니어
    private double reputationScore;  // 사용자 평판 점수

    private double matchScore;       // 추천 점수
}