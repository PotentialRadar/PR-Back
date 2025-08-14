package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {

    List<ProjectComment> findByProject_ProjectId(Long projectId);
}
