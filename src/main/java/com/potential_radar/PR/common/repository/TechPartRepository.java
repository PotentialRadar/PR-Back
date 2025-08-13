package com.potential_radar.PR.common.repository;

import com.potential_radar.PR.common.entity.TechPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TechPartRepository extends JpaRepository<TechPart, Long> {
}
