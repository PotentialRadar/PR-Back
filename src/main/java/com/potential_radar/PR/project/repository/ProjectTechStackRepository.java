package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectTechStack;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProjectTechStackRepository extends JpaRepository<ProjectTechStack, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectTechStack s where s.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
