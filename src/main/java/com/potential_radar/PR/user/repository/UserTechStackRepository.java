package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserTechStack1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTechStackRepository extends JpaRepository<UserTechStack1, Long> {
    
    List<UserTechStack1> findByUser(User user);
    
    List<UserTechStack1> findByUserUserId(Long userId);
    
    Optional<UserTechStack1> findByUserAndUserTechStackId(User user, Long userTechStackId);
    
    @Query("SELECT uts FROM UserTechStack1 uts " +
           "JOIN FETCH uts.stack " +
           "WHERE uts.user = :user")
    List<UserTechStack1> findByUserWithTechStack(@Param("user") User user);
    
    boolean existsByUserAndStack_StackId(User user, Long stackId);
    
    void deleteByUserAndUserTechStackId(User user, Long userTechStackId);
}