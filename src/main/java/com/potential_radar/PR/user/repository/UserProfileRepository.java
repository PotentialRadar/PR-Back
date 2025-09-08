package com.potential_radar.PR.user.repository;

import com.potential_radar.PR.user.domain.ExperienceRange;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(User user);
    
    // 공개된 포트폴리오만 조회 (isPortfolioOpen = true)
    @Query("SELECT up FROM UserProfile up WHERE up.userId = :userId AND up.isPortfolioOpen = true AND up.isSearchOpen = true")
    Optional<UserProfile> findPublicPortfolioByUserId(@Param("userId") Long userId);
    
    // 공개된 모든 포트폴리오 조회 (페이징)
    @Query("SELECT up FROM UserProfile up WHERE up.isPortfolioOpen = true AND up.isSearchOpen = true")
    Page<UserProfile> findAllPublicPortfolios(Pageable pageable);
    
    // 기술 파트별 공개 포트폴리오 조회
    @Query("SELECT up FROM UserProfile up WHERE up.isPortfolioOpen = true AND up.isSearchOpen = true AND up.techPart.name = :techPartName")
    Page<UserProfile> findPublicPortfoliosByTechPart(@Param("techPartName") String techPartName, Pageable pageable);
    
    // 경력 범위별 공개 포트폴리오 조회
    @Query("SELECT up FROM UserProfile up WHERE up.isPortfolioOpen = true AND up.isSearchOpen = true AND up.experienceRange = :experienceRange")
    Page<UserProfile> findPublicPortfoliosByExperienceRange(@Param("experienceRange") ExperienceRange experienceRange, Pageable pageable);
    
    // 키워드 검색 (닉네임, 자기소개, 직책에서 검색)
    @Query("""
        SELECT up FROM UserProfile up 
        WHERE up.isPortfolioOpen = true AND up.isSearchOpen = true 
        AND (LOWER(up.user.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(up.bio) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(up.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<UserProfile> findPublicPortfoliosByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 복합 검색 (기술 파트 + 키워드)
    @Query("""
        SELECT up FROM UserProfile up 
        WHERE up.isPortfolioOpen = true AND up.isSearchOpen = true 
        AND (:techPartName IS NULL OR up.techPart.name = :techPartName)
        AND (:experienceRange IS NULL OR up.experienceRange = :experienceRange)
        AND (:keyword IS NULL OR 
             LOWER(up.user.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(up.bio) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(up.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<UserProfile> findPublicPortfoliosWithFilters(
            @Param("techPartName") String techPartName,
            @Param("experienceRange") ExperienceRange experienceRange, 
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
