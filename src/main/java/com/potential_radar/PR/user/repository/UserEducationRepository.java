package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.UserEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserEducationRepository extends JpaRepository<UserEducation, Long> {
    
    @Query("SELECT e FROM UserEducation e WHERE e.user.userId = :userId ORDER BY e.startDate DESC")
    List<UserEducation> findByUserIdOrderByStartDateDesc(@Param("userId") Long userId);
    
    @Query("SELECT e FROM UserEducation e WHERE e.user.userId = :userId AND e.isCurrent = true")
    List<UserEducation> findCurrentEducationByUserId(@Param("userId") Long userId);
}