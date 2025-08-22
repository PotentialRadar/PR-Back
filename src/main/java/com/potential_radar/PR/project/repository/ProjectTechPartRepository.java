package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectTechPart;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectTechPartRepository extends JpaRepository<ProjectTechPart, Long> {
    List<ProjectTechPart> findByProject_ProjectId(Long projectId);

    // 벌크 삭제: 컬렉션 로딩 없이 곧바로 DELETE
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectTechPart p where p.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    boolean existsByProject_ProjectIdAndTechPart_Name(Long projectId, String techPartName);
}
