package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    @Query("SELECT COUNT(p) FROM ProjectRecruitment p WHERE p.updatedAt >= :since")
    long countByUpdatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM ProjectRecruitment p WHERE p.updatedAt >= :since ORDER BY p.updatedAt ASC")
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

    // 인기순 정렬 (좋아요 수 내림차순, 생성 시간 내림차순)
    @Query("SELECT p FROM ProjectRecruitment p ORDER BY (SELECT count(l.id) FROM Like l WHERE l.targetId = p.projectId AND l.targetType = 'PROJECT') DESC, p.createdAt DESC")
    Page<ProjectRecruitment> findAllOrderByLikeCountAndCreatedAt(Pageable pageable);

    // 마감임박순 정렬
    Page<ProjectRecruitment> findAllByOrderByRecruitDeadlineAsc(Pageable pageable);

    // 지원마감일과 상태로 프로젝트 조회
    List<ProjectRecruitment> findByRecruitDeadlineAndStatus(LocalDate recruitDeadline, ProjectStatus status);

    // 종료일과 상태로 프로젝트 조회
    List<ProjectRecruitment> findByEndDateAndStatus(LocalDate endDate, ProjectStatus status);

    // 상태와 모집마감일로 프로젝트 조회 (마감일이 지난)
    List<ProjectRecruitment> findByStatusAndRecruitDeadlineBefore(ProjectStatus status, LocalDate date);

    // 상태와 종료일로 프로젝트 조회 (종료일이 지난)
    List<ProjectRecruitment> findByStatusAndEndDateBefore(ProjectStatus status, LocalDate date);

    // 증분 동기화용 - 기본 프로젝트와 팀리더만 로딩 (기술스택, 기술파트는 lazy loading)
    @Query("SELECT DISTINCT p FROM ProjectRecruitment p " +
           "LEFT JOIN FETCH p.teamLeader " +
           "WHERE p.updatedAt >= :since ORDER BY p.updatedAt ASC")
    List<ProjectRecruitment> findProjectsModifiedAfter(@Param("since") LocalDateTime since);

    // 증분 동기화용 - 기술스택만 함께 로딩
    @Query("SELECT DISTINCT p FROM ProjectRecruitment p " +
           "LEFT JOIN FETCH p.techStacks pts LEFT JOIN FETCH pts.techStack " +
           "WHERE p.projectId IN :projectIds")
    List<ProjectRecruitment> findProjectsWithTechStacks(@Param("projectIds") List<Long> projectIds);

    // 증분 동기화용 - 기술파트만 함께 로딩  
    @Query("SELECT DISTINCT p FROM ProjectRecruitment p " +
           "LEFT JOIN FETCH p.techParts ptp LEFT JOIN FETCH ptp.techPart " +
           "WHERE p.projectId IN :projectIds")
    List<ProjectRecruitment> findProjectsWithTechParts(@Param("projectIds") List<Long> projectIds);

}
