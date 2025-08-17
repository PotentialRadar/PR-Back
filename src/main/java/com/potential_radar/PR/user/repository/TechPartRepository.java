package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.model.TechPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechPartRepository extends JpaRepository<TechPart, Long> {
}