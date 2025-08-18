package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {

    List<ProjectComment> findByProject_ProjectId(Long projectId);

    @Modifying
    @Query("delete from ProjectComment pc where pc.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
