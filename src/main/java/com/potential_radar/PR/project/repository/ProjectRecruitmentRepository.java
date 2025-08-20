package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRecruitmentRepository extends JpaRepository<ProjectRecruitment, Long> {
    @Query("SELECT pr FROM ProjectRecruitment pr JOIN FETCH pr.teamLeader WHERE pr.projectId = :projectId")
    Optional<ProjectRecruitment> findByIdWithTeamLeader(@Param("projectId") Long projectId);

    List<ProjectRecruitment> findByTeamLeader_UserId(Long teamLeaderId);
}
