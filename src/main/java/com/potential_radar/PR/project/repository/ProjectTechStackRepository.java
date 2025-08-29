package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectTechStack;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectTechStackRepository extends JpaRepository<ProjectTechStack, Long> {

//    List<ProjectTechStack> findByProject_ProjectId(Long projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectTechStack s where s.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
    
    // 특정 프로젝트의 기술스택 목록 조회
    List<ProjectTechStack> findByProject(ProjectRecruitment project);
    
    // 프로젝트 ID로 기술스택 목록 조회 (대안 메서드)
    @Query("SELECT pts FROM ProjectTechStack pts WHERE pts.project.projectId = :projectId")
    List<ProjectTechStack> findByProject_ProjectId(@Param("projectId") Long projectId);
}
