package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProject_ProjectId(Long projectId);

    boolean existsByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);
}
