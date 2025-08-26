package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, Long> {
    
    List<PortfolioProject> findAllByUser_UserId(Long userId);
    
    @Query("SELECT pp.project.projectId FROM PortfolioProject pp WHERE pp.user.userId = :userId")
    List<Long> findProjectIdsByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM PortfolioProject pp WHERE pp.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    boolean existsByUser_UserIdAndProject_ProjectId(Long userId, Long projectId);
}