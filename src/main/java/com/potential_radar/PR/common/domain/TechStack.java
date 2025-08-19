package com.potential_radar.PR.common.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tech_stack")
@Getter @NoArgsConstructor
public class TechStack {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_stack_id")
    private Long id;

    @Column(name = "tech_stack_name", nullable = false, unique = true)
    private String name;
}

