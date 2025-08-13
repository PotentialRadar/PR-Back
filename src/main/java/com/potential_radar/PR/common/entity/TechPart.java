package com.potential_radar.PR.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

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
