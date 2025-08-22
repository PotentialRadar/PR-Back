package com.potential_radar.PR.tech.repository;

import com.potential_radar.PR.tech.domain.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    Optional<TechStack> findByNameIgnoreCase(String name);
}
