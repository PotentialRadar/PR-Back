package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.tech.entity.TechPart;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTechPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectTechPartId;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectRecruitment project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_part_id")
    private TechPart techPart; // 예: 백엔드, 프론트엔드, 모바일 등

    @Column(nullable = false)
    private Integer recruitCount; // 기술별 정원
}
