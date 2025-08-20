package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 프로젝트
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectRecruitment project;

    // 멤버(유저)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 역할: 팀리더/일반멤버
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 어떤 파트로 합류했는지 (선택)
    @Column(name = "tech_part")
    private String techPart;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void onCreate() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
        if (role == null) role = Role.MEMBER;
    }

    public enum Role { LEADER, MEMBER }
}
