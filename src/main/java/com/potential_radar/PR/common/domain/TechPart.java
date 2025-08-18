package com.potential_radar.PR.common.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

@Entity
@Getter
@NoArgsConstructor
@ToString
@Table(name = "tech_part")
public class TechPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Builder
    public TechPart(String name) {
        this.name = name;
    }
}
