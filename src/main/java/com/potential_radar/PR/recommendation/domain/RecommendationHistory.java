package com.potential_radar.PR.recommendation.domain;

import com.potential_radar.PR.common.domain.BaseTimeEntity;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 추천 받은 사용자
    private User user; // 추천을 받은 사용자

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false)
    private RecommendationType recommendationType; // 추천 타입

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_project_id")
    private ProjectRecruitment recommendedProject; // 추천된 프로젝트 (PROJECT 타입일 때만)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_user_id")
    private User recommendedMember; // 추천된 사용자 (MEMBER 타입일 때만)

    @Column(name = "project_context_id") // 팀원 추천 시 어떤 프로젝트를 위한 추천인지
    private Long projectContextId;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "is_clicked", nullable = false)
    private boolean isClicked = false;

    @Column(name = "is_converted", nullable = false)
    private boolean isConverted = false; // 지원/좋아요/초대 등 전환 여부

    @Builder
    public RecommendationHistory(User user, RecommendationType recommendationType, 
                                ProjectRecruitment recommendedProject, User recommendedMember, 
                                Long projectContextId, Double matchScore, String modelVersion) {
        this.user = user;
        this.recommendationType = recommendationType;
        this.recommendedProject = recommendedProject;
        this.recommendedMember = recommendedMember;
        this.projectContextId = projectContextId;
        this.matchScore = matchScore;
        this.modelVersion = modelVersion;
    }

    // 클릭 이벤트 기록
    public void markAsClicked() {
        this.isClicked = true;
    }

    // 전환 이벤트 기록 (지원, 좋아요, 초대 등)
    public void markAsConverted() {
        this.isConverted = true;
    }
}