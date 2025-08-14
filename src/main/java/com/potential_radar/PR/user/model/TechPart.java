package com.potential_radar.PR.user.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "tech_part")
public class TechPart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long techPartId;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public TechPart(String name) {
        this.name = name;
    }
}
