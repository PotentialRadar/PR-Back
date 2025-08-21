package com.potential_radar.PR.user.domain;

import com.potential_radar.PR.tech.domain.TechStack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_tech_stack")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTechStackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id")
    private TechStack techStack;

    @Column(nullable = true)
    private Integer skillLevel;
}

