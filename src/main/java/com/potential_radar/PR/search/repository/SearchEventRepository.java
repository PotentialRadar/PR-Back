package com.potential_radar.PR.search.repository;

import com.potential_radar.PR.search.domain.SearchEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SearchEventRepository extends JpaRepository<SearchEvent, Long> {
    
    @Query("SELECT s.keyword, COUNT(s) as count FROM SearchEvent s " +
           "WHERE s.keyword IS NOT NULL AND s.searchTime > :since " +
           "GROUP BY s.keyword ORDER BY COUNT(s) DESC")
    List<Object[]> findPopularKeywords(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT s.singleTechStack, COUNT(s) as count FROM SearchEvent s " +
           "WHERE s.singleTechStack IS NOT NULL AND s.searchTime > :since " +
           "GROUP BY s.singleTechStack ORDER BY COUNT(s) DESC")
    List<Object[]> findPopularTechStacks(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT s.techPart, COUNT(s) as count FROM SearchEvent s " +
           "WHERE s.techPart IS NOT NULL AND s.searchTime > :since " +
           "GROUP BY s.techPart ORDER BY COUNT(s) DESC")
    List<Object[]> findPopularTechParts(@Param("since") LocalDateTime since, Pageable pageable);

    // 포트폴리오(사용자) 검색 전용 쿼리들
    @Query("SELECT s.keyword, COUNT(s) as count FROM SearchEvent s " +
           "WHERE s.keyword IS NOT NULL AND s.searchTime > :since AND s.searchType = 'user' " +
           "GROUP BY s.keyword ORDER BY COUNT(s) DESC")
    List<Object[]> findPopularUserKeywords(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT s.singleTechStack, COUNT(s) as count FROM SearchEvent s " +
           "WHERE s.singleTechStack IS NOT NULL AND s.searchTime > :since AND s.searchType = 'user' " +
           "GROUP BY s.singleTechStack ORDER BY COUNT(s) DESC")
    List<Object[]> findPopularUserTechStacks(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT s.techPart, COUNT(s) as count FROM SearchEvent s " +
           "WHERE s.techPart IS NOT NULL AND s.searchTime > :since AND s.searchType = 'user' " +
           "GROUP BY s.techPart ORDER BY COUNT(s) DESC")
    List<Object[]> findPopularUserTechParts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM SearchEvent s WHERE s.searchTime > :since")
    long countRecentSearches(@Param("since") LocalDateTime since);
}