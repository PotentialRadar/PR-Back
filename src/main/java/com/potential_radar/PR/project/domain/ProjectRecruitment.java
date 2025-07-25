package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_recruitment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectRecruitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_leader_id")
    private User teamLeader;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    private LocalDate recruitDeadline;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.RECRUITING;

    private Integer viewCount = 0;

    @Column(name = "file_url")
    private String fileUrl; // 첨부파일 경로

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // 연관관계(기술스택)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTechStack> techStacks = new ArrayList<>();

    @Column(nullable = false)
    private Integer recruitCount;
}
