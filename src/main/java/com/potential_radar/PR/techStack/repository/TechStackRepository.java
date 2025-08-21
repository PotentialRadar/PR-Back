package com.potential_radar.PR.techStack.repository;

import com.potential_radar.PR.techStack.domain.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    
    Optional<TechStack> findByName(String name);
    
    boolean existsByName(String name);
}