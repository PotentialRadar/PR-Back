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
public class TechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long techStackId;

    @Column(nullable = false, unique = true)
    private String name;  // React, Spring Boot, Python 등
}
