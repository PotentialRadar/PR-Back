package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.common.domain.TechPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechPartRepository extends JpaRepository<TechPart, Long> {
}