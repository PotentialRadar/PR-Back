package com.potential_radar.PR.project.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "project_tech_part",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_part", columnNames = {"project_id", "part_name"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTechPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 프로젝트 다대일
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_project_part_project"))
    private ProjectRecruitment project;

    // FRONTEND / BACKEND / DEVOPS
    @Column(name = "part_name", nullable = false, length = 32)
    private String partName;

    // 파트별 모집 정원
    @Column(name = "recruit_count", nullable = false)
    private Integer recruitCount;
}
