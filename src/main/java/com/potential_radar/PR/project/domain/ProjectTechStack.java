package com.potential_radar.PR.project.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_tech_stack")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectRecruitment project;

    @Column(nullable = false)
    private String techStackName; // 예: Java, Spring, Vue

    @Column(nullable = false)
    private Integer recruitCount; // 기술별 정원
}
