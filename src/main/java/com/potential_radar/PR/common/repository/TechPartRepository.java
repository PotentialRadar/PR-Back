package com.potential_radar.PR.common.repository;

import com.potential_radar.PR.common.domain.TechPart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechPartRepository extends JpaRepository<TechPart, Long> {
    Optional<TechPart> findByNameIgnoreCase(String name);
}
