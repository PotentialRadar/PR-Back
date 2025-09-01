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
    
    // 사용자들이 많이 사용하는 기술스택 TOP N 조회 (사용 빈도 기반)
    @Query("SELECT ts.name, COUNT(*) as usageCount " +
           "FROM UserTechStack uts " +
           "JOIN uts.stack ts " +
           "GROUP BY ts.name " +
           "ORDER BY usageCount DESC")
    List<Object[]> findMostUsedTechStacks();
}