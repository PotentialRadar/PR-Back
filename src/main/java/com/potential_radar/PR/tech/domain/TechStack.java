package com.potential_radar.PR.tech.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "tech_stack",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tech_stack_name", columnNames = "name")
        }
)
public class TechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long techStackId;

    @Column(nullable = false, unique = true)
    private String name;  // React, Spring Boot, Python 등
}
