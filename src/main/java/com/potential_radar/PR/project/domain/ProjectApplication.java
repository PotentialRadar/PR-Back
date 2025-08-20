package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_application",    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 프로젝트
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectRecruitment project;

    // 지원자(유저)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "tech_part")
    private String techPart;

    @Column(name = "application_message", length = 255)
    private String applicationMessage;

    // 상태(지원, 합류 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    public enum ApplicationStatus {
        PENDING,   // 지원중
        ACCEPTED,  // 합류됨
        REJECTED   // 거절
    }
}