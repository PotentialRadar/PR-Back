package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserTechStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, Long> {
    
    List<UserTechStack> findByUser(User user);
    
    List<UserTechStack> findByUserUserId(Long userId);
    
    Optional<UserTechStack> findByUserAndUserTechStackId(User user, Long userTechStackId);
    
    @Query("SELECT uts FROM UserTechStack uts " +
           "JOIN FETCH uts.stack " +
           "WHERE uts.user = :user")
    List<UserTechStack> findByUserWithTechStack(@Param("user") User user);
    
    boolean existsByUserAndStack_TechStackId(User user, Long techStackId);
    
    void deleteByUserAndUserTechStackId(User user, Long userTechStackId);
}