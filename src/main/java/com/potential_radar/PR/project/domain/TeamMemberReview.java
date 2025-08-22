package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_member_review", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "reviewer_id", "reviewee_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리뷰 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // 리뷰 받은 사람 (ERD의 '회원 ID')
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    // 리뷰 대상 프로젝트 (ERD의 '프로젝트 ID')
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectRecruitment project;

    // 평점 (ERD의 '평점')
    @Column(nullable = false)
    private Integer rating;

    // 리뷰 내용
    @Column(length = 1000)
    private String comment;

    // 작성일 (ERD의 '작성일')
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
