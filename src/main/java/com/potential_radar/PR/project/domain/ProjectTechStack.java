package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.tech.domain.TechStack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "project_tech_stack",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_project_tech_stack",
                columnNames = {"project_id", "tech_stack_id"}
        ),
        indexes = {
                @Index(name = "idx_pts_project", columnList = "project_id"),
                @Index(name = "idx_pts_stack",   columnList = "tech_stack_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pts_project"))
    private ProjectRecruitment project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tech_stack_id", nullable = false,   // ← "tech_stack id"(X) 공백 없는 이름(O)
            foreignKey = @ForeignKey(name = "fk_pts_stack"))
    private TechStack techStack;

    @Column(name = "recruit_count", nullable = false)
    private Integer recruitCount; // 기술별 정원
}
