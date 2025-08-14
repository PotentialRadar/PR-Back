package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectTechPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTechPartRepository extends JpaRepository<ProjectTechPart, Long> {
    List<ProjectTechPart> findByProject_ProjectId(Long projectId);
    void deleteByProject_ProjectId(Long projectId);
    boolean existsByProject_ProjectIdAndPartName(Long projectId, String partName);
}
