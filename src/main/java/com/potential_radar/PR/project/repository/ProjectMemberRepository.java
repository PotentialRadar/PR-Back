package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);

    List<ProjectMember> findAllByProject_ProjectId(Long projectId);

    List<ProjectMember> findAllByUser_UserId(Long userId);

    long countByProject_ProjectId(Long projectId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProjectMember m where m.project.projectId = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);
}
