package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {
    List<ProjectApplication> findByProject_ProjectId(Long projectId);
    int countByProject_ProjectId(Long projectId);
    int countByProject_ProjectIdAndStatus(Long projectId, ProjectApplication.ApplicationStatus status);

    boolean existsByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectApplication a where a.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}