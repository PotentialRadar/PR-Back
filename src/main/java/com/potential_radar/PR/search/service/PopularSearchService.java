package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.repository.SearchEventRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
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
    
    @Value("${app.popular-search.cache-duration-hours:168}")
    private int cacheDurationHours;  // 기본값을 1주일(168시간)로 변경
    
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
    
    // 애플리케이션 시작 시 기술스택 데이터 즉시 캐싱
    @EventListener(ApplicationReadyEvent.class)
    public void initializePopularItemsOnStartup() {
        log.info("Initializing popular search items on application startup...");
        try {
            // 기술스택만 즉시 초기화 (가장 중요한 필터 태그)
            updatePopularTechStacks();
            updatePopularUserTechStacks();
            log.info("Popular tech stacks initialized successfully on startup");
            
            // 다른 항목들도 초기화 (선택사항)
            updatePopularKeywords();
            updatePopularTechParts();
            updatePopularUserKeywords();
            updatePopularUserTechParts();
            log.info("All popular search items initialized successfully on startup");
        } catch (Exception e) {
            log.error("Failed to initialize popular search items on startup: {}", e.getMessage());
            // 실패해도 기본값으로 대체하므로 애플리케이션 시작을 방해하지 않음
        }
    }

    @Scheduled(cron = "0 0 2 * * SUN")  // 매주 일요일 새벽 2시
    public void updatePopularItems() {
        log.info("Weekly update of popular search items... ({}일 기준, 최소 {}회 검색)", 
                popularSearchDays, minSearchCount);
        updatePopularKeywords();
        updatePopularTechStacks();
        updatePopularTechParts();
        // 포트폴리오(사용자) 검색 전용 업데이트
        updatePopularUserKeywords();
        updatePopularUserTechStacks();
        updatePopularUserTechParts();
        log.info("Weekly popular search items update completed successfully");
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
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
            
            // 캐시된 데이터가 없으면 기본 인기 기술스택 반환
            log.warn("No cached popular tech stacks found, returning default popular tech stacks for projects");
            return getDefaultPopularTechStacks();
        } catch (Exception e) {
            log.warn("Failed to get popular tech stacks from cache: {}", e.getMessage());
            return getDefaultPopularTechStacks();
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
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
            
            // 캐시된 데이터가 없으면 기본 인기 기술스택 반환
            log.warn("No cached popular user tech stacks found, returning default popular tech stacks for users");
            return getDefaultPopularUserTechStacks();
        } catch (Exception e) {
            log.warn("Failed to get popular user tech stacks from cache: {}", e.getMessage());
            return getDefaultPopularUserTechStacks();
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
    
    // 기본 프로젝트 기반 인기 기술스택 (캐시 실패 시 사용)
    private List<String> getDefaultPopularTechStacks() {
        return Arrays.asList(
            "JavaScript", "React", "Vue.js", "Node.js", "TypeScript",
            "Java", "Spring Boot", "Spring", "Python", "Django",
            "MySQL", "PostgreSQL", "MongoDB", "AWS", "Docker", 
            "Git", "HTML/CSS", "Express.js", "Next.js", "Flutter"
        );
    }
    
    // 기본 사용자 기반 인기 기술스택 (캐시 실패 시 사용)  
    private List<String> getDefaultPopularUserTechStacks() {
        return Arrays.asList(
            "JavaScript", "React", "Java", "Python", "Vue.js",
            "Spring Boot", "Node.js", "TypeScript", "MySQL", "Spring",
            "HTML/CSS", "Git", "AWS", "PostgreSQL", "Express.js",
            "Django", "Docker", "MongoDB", "Next.js", "Flutter"
        );
    }
}