package com.potential_radar.PR.tech.repository;

import com.potential_radar.PR.tech.domain.TechPart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface TechPartRepository extends JpaRepository<TechPart, Long> {
    Optional<TechPart> findByNameIgnoreCase(String name);
}
