package com.potential_radar.PR.recommendation.domain;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RecommendationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 추천 받은 사용자
    private User user; // 추천을 받은 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_project_id")
    private ProjectRecruitment recommendedProject; // 추천된 프로젝트

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_user_id")
    private User recommendedMember; // 추천된 사용자

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "is_clicked", nullable = false)
    private boolean isClicked = false;

    @Column(name = "is_converted", nullable = false)
    private boolean isConverted = false; // 지원/좋아요 등 전환 여부

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RecommendationHistory(User user, ProjectRecruitment recommendedProject, User recommendedMember, Double matchScore, String modelVersion) {
        this.user = user;
        this.recommendedProject = recommendedProject;
        this.recommendedMember = recommendedMember;
        this.matchScore = matchScore;
        this.modelVersion = modelVersion;
    }
}