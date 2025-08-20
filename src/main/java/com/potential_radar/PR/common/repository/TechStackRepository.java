package com.potential_radar.PR.common.repository;

import com.potential_radar.PR.common.domain.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    Optional<TechStack> findByNameIgnoreCase(String name);
}
