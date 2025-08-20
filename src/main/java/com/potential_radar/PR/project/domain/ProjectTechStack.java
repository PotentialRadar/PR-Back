package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.tech.entity.TechStack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_tech_stack")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectTechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectTechStackId;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectRecruitment project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id")
    private TechStack techStack; // 예: Java, Spring, Vue


}
