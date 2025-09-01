package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.repository.SearchEventRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularSearchService {
    
    private final SearchEventRepository searchEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final UserTechStackRepository userTechStackRepository;
    
    // 설정 값들
    @Value("${app.popular-search.days:30}")
    private int popularSearchDays;
    
    @Value("${app.popular-search.min-search-count:5}")
    private int minSearchCount;
    
    @Value("${app.popular-search.cache-duration-hours:1}")
    private int cacheDurationHours;
    
    @Value("${app.popular-search.max-keywords:20}")
    private int maxKeywords;
    
    @Value("${app.popular-search.max-tech-stacks:15}")
    private int maxTechStacks;
    
    @Value("${app.popular-search.max-tech-parts:10}")
    private int maxTechParts;
    
    private static final String POPULAR_KEYWORDS_KEY = "popular:keywords";
    private static final String POPULAR_TECH_STACKS_KEY = "popular:techstacks";
    private static final String POPULAR_TECH_PARTS_KEY = "popular:techparts";
    
    // 포트폴리오(사용자) 검색 전용 키들
    private static final String POPULAR_USER_KEYWORDS_KEY = "popular:user:keywords";
    private static final String POPULAR_USER_TECH_STACKS_KEY = "popular:user:techstacks";
    private static final String POPULAR_USER_TECH_PARTS_KEY = "popular:user:techparts";
    
    @Scheduled(fixedRateString = "${app.popular-search.update-interval:3600000}")
    public void updatePopularItems() {
        log.info("Updating popular search items... ({}일 기준, 최소 {}회 검색)", 
                popularSearchDays, minSearchCount);
        updatePopularKeywords();
        updatePopularTechStacks();
        updatePopularTechParts();
        // 포트폴리오(사용자) 검색 전용 업데이트
        updatePopularUserKeywords();
        updatePopularUserTechStacks();
        updatePopularUserTechParts();
        log.info("Popular search items updated successfully");
    }
    
    private void updatePopularKeywords() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(popularSearchDays);
            PageRequest pageRequest = PageRequest.of(0, maxKeywords);
            
            List<Object[]> results = searchEventRepository.findPopularKeywords(since, pageRequest);
            
            // 최소 검색 횟수 필터링 추가
            List<String> popularKeywords = results.stream()
                .filter(row -> ((Long) row[1]) >= minSearchCount) // 최소 검색 횟수 체크
                .map(row -> {
                    String keyword = (String) row[0];
                    Long count = (Long) row[1];
                    log.debug("Popular keyword: {} ({}회)", keyword, count);
                    return keyword;
                })
                .collect(Collectors.toList());
                
            Duration cacheDuration = Duration.ofHours(cacheDurationHours);
            redisTemplate.opsForValue().set(POPULAR_KEYWORDS_KEY, popularKeywords, cacheDuration);
            log.info("Updated {} popular keywords ({}+ searches in {} days)", 
                    popularKeywords.size(), minSearchCount, popularSearchDays);
        } catch (Exception e) {
            log.error("Failed to update popular keywords: {}", e.getMessage());
        }
    }
    
    private void updatePopularTechStacks() {
        try {
            // 프로젝트에서 많이 사용하는 기술스택 조회 (사용 빈도 기반)
            List<Object[]> results = projectTechStackRepository.findMostUsedTechStacks();
            
            List<String> popularTechStacks = results.stream()
                .limit(maxTechStacks) // 최대 개수 제한
                .map(row -> {
                    String techStack = (String) row[0];
                    Long count = (Long) row[1];
                    log.debug("Most used project tech stack: {} ({}회 사용)", techStack, count);
                    return techStack;
                })
                .collect(Collectors.toList());
                
            Duration cacheDuration = Duration.ofHours(cacheDurationHours);
            redisTemplate.opsForValue().set(POPULAR_TECH_STACKS_KEY, popularTechStacks, cacheDuration);
            log.info("Updated {} most used project tech stacks", popularTechStacks.size());
        } catch (Exception e) {
            log.error("Failed to update popular tech stacks: {}", e.getMessage());
        }
    }
    
    private void updatePopularTechParts() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(popularSearchDays);
            PageRequest pageRequest = PageRequest.of(0, maxTechParts);
            
            List<Object[]> results = searchEventRepository.findPopularTechParts(since, pageRequest);
            
            // 최소 검색 횟수 필터링 추가
            List<String> popularTechParts = results.stream()
                .filter(row -> ((Long) row[1]) >= minSearchCount) // 최소 검색 횟수 체크
                .map(row -> {
                    String techPart = (String) row[0];
                    Long count = (Long) row[1];
                    log.debug("Popular tech part: {} ({}회)", techPart, count);
                    return techPart;
                })
                .collect(Collectors.toList());
                
            Duration cacheDuration = Duration.ofHours(cacheDurationHours);
            redisTemplate.opsForValue().set(POPULAR_TECH_PARTS_KEY, popularTechParts, cacheDuration);
            log.info("Updated {} popular tech parts ({}+ searches in {} days)", 
                    popularTechParts.size(), minSearchCount, popularSearchDays);
        } catch (Exception e) {
            log.error("Failed to update popular tech parts: {}", e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPopularKeywords() {
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(POPULAR_KEYWORDS_KEY);
            return cached != null ? cached : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get popular keywords from cache: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPopularTechStacks() {
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(POPULAR_TECH_STACKS_KEY);
            return cached != null ? cached : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get popular tech stacks from cache: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPopularTechParts() {
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(POPULAR_TECH_PARTS_KEY);
            return cached != null ? cached : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get popular tech parts from cache: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    // 포트폴리오(사용자) 검색 전용 메서드들
    private void updatePopularUserKeywords() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(popularSearchDays);
            PageRequest pageRequest = PageRequest.of(0, maxKeywords);
            
            List<Object[]> results = searchEventRepository.findPopularUserKeywords(since, pageRequest);
            
            List<String> popularUserKeywords = results.stream()
                .filter(row -> ((Long) row[1]) >= minSearchCount)
                .map(row -> {
                    String keyword = (String) row[0];
                    Long count = (Long) row[1];
                    log.debug("Popular user keyword: {} ({}회)", keyword, count);
                    return keyword;
                })
                .collect(Collectors.toList());
                
            Duration cacheDuration = Duration.ofHours(cacheDurationHours);
            redisTemplate.opsForValue().set(POPULAR_USER_KEYWORDS_KEY, popularUserKeywords, cacheDuration);
            log.info("Updated {} popular user keywords ({}+ searches in {} days)", 
                    popularUserKeywords.size(), minSearchCount, popularSearchDays);
        } catch (Exception e) {
            log.error("Failed to update popular user keywords: {}", e.getMessage());
        }
    }
    
    private void updatePopularUserTechStacks() {
        try {
            // 사용자들이 많이 사용하는 기술스택 조회 (사용 빈도 기반)
            List<Object[]> results = userTechStackRepository.findMostUsedTechStacks();
            
            List<String> popularUserTechStacks = results.stream()
                .limit(maxTechStacks) // 최대 개수 제한
                .map(row -> {
                    String techStack = (String) row[0];
                    Long count = (Long) row[1];
                    log.debug("Most used user tech stack: {} ({}명 사용)", techStack, count);
                    return techStack;
                })
                .collect(Collectors.toList());
                
            Duration cacheDuration = Duration.ofHours(cacheDurationHours);
            redisTemplate.opsForValue().set(POPULAR_USER_TECH_STACKS_KEY, popularUserTechStacks, cacheDuration);
            log.info("Updated {} most used user tech stacks", popularUserTechStacks.size());
        } catch (Exception e) {
            log.error("Failed to update popular user tech stacks: {}", e.getMessage());
        }
    }
    
    private void updatePopularUserTechParts() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(popularSearchDays);
            PageRequest pageRequest = PageRequest.of(0, maxTechParts);
            
            List<Object[]> results = searchEventRepository.findPopularUserTechParts(since, pageRequest);
            
            List<String> popularUserTechParts = results.stream()
                .filter(row -> ((Long) row[1]) >= minSearchCount)
                .map(row -> {
                    String techPart = (String) row[0];
                    Long count = (Long) row[1];
                    log.debug("Popular user tech part: {} ({}회)", techPart, count);
                    return techPart;
                })
                .collect(Collectors.toList());
                
            Duration cacheDuration = Duration.ofHours(cacheDurationHours);
            redisTemplate.opsForValue().set(POPULAR_USER_TECH_PARTS_KEY, popularUserTechParts, cacheDuration);
            log.info("Updated {} popular user tech parts ({}+ searches in {} days)", 
                    popularUserTechParts.size(), minSearchCount, popularSearchDays);
        } catch (Exception e) {
            log.error("Failed to update popular user tech parts: {}", e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPopularUserKeywords() {
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(POPULAR_USER_KEYWORDS_KEY);
            return cached != null ? cached : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get popular user keywords from cache: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPopularUserTechStacks() {
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(POPULAR_USER_TECH_STACKS_KEY);
            return cached != null ? cached : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get popular user tech stacks from cache: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getPopularUserTechParts() {
        try {
            List<String> cached = (List<String>) redisTemplate.opsForValue().get(POPULAR_USER_TECH_PARTS_KEY);
            return cached != null ? cached : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get popular user tech parts from cache: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}