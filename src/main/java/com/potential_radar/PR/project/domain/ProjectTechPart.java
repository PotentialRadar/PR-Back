package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.common.domain.TechPart;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "project_tech_part",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_project_part",
                columnNames = {"project_id", "tech_part_id"} // ← part_name(X) tech_part_id(O)
        ),
        indexes = {
                @Index(name = "idx_ptp_project", columnList = "project_id"),
                @Index(name = "idx_ptp_part",    columnList = "tech_part_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTechPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 프로젝트 다대일
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptp_project"))
    private ProjectRecruitment project;

    // FRONTEND / BACKEND / DEVOPS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tech_part_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptp_part"))
    private TechPart techPart;

    // 파트별 모집 정원
    @Column(name = "recruit_count", nullable = false)
    private Integer recruitCount;
}