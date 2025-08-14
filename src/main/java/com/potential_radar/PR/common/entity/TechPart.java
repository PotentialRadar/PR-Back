package com.potential_radar.PR.common.entity;

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
public class TechPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long techPartId;

    @Column(nullable = false, unique = true)
    private String name;
}
