package com.potential_radar.PR.techStack.repository;

import com.potential_radar.PR.techStack.domain.TechStackToDelete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStackToDelete, Long> {
    
    Optional<TechStackToDelete> findByName(String name);
    
    boolean existsByName(String name);
}