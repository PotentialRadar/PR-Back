package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRecruitmentRepository extends JpaRepository<ProjectRecruitment, Long> {
    
    // 증분 동기화를 위한 메서드들
    List<ProjectRecruitment> findByUpdatedAtAfter(LocalDateTime since);
    
    List<ProjectRecruitment> findByUpdatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(p) FROM ProjectRecruitment p WHERE p.updatedAt > :since")
    long countByUpdatedAtAfter(@Param("since") LocalDateTime since);
    
    @Query("SELECT p FROM ProjectRecruitment p WHERE p.updatedAt > :since ORDER BY p.updatedAt ASC")
    List<ProjectRecruitment> findByUpdatedAtAfterOrderByUpdatedAt(@Param("since") LocalDateTime since);
    
    // 페이징 지원 증분 동기화
    List<ProjectRecruitment> findByUpdatedAtAfterOrderByUpdatedAt(LocalDateTime since, PageRequest pageRequest);
    
    // 상태별 조회
    List<ProjectRecruitment> findByStatusAndUpdatedAtAfter(ProjectStatus status, LocalDateTime since);
    
    // 모집중인 프로젝트만 조회
    List<ProjectRecruitment> findByStatus(ProjectStatus status);

    @Query("SELECT p FROM ProjectRecruitment p JOIN FETCH p.techStacks")
    List<ProjectRecruitment> findAllWithTechStacks();

    @Query("SELECT DISTINCT p FROM ProjectRecruitment p LEFT JOIN FETCH p.techStacks")
    List<ProjectRecruitment> findAllWithTechStacksOnly();

    @Query("SELECT DISTINCT p FROM ProjectRecruitment p LEFT JOIN FETCH p.techParts")
    List<ProjectRecruitment> findAllWithTechPartsOnly();

    @Query("SELECT pr FROM ProjectRecruitment pr JOIN FETCH pr.teamLeader WHERE pr.projectId = :projectId")
    Optional<ProjectRecruitment> findByIdWithTeamLeader(@Param("projectId") Long projectId);

    List<ProjectRecruitment> findByTeamLeader_UserId(Long teamLeaderId);
}
