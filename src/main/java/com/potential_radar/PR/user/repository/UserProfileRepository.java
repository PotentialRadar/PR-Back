package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(User user);
    
    // 공개된 포트폴리오만 조회 (isPortfolioOpen = true)
    @Query("SELECT up FROM UserProfile up WHERE up.userId = :userId AND up.isPortfolioOpen = true")
    Optional<UserProfile> findPublicPortfolioByUserId(@Param("userId") Long userId);
}
