package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.UserExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserExperienceRepository extends JpaRepository<UserExperience, Long> {
    
    @Query("SELECT e FROM UserExperience e WHERE e.user.userId = :userId ORDER BY e.startDate DESC")
    List<UserExperience> findByUserIdOrderByStartDateDesc(@Param("userId") Long userId);
    
    @Query("SELECT e FROM UserExperience e WHERE e.user.userId = :userId AND e.isCurrent = true")
    List<UserExperience> findCurrentExperienceByUserId(@Param("userId") Long userId);
}