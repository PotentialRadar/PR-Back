package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
import com.potential_radar.PR.search.dto.UserSearchReq;
import com.potential_radar.PR.search.dto.UserSearchRes;
import com.potential_radar.PR.search.dto.SearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.List;

/**
 * 스마트 캐싱 전략을 구현하는 서비스
 * - 인기 검색어/기술스택: 1주일 TTL (안정적 데이터)
 * - 인기 검색 결과: 5분 TTL + 이벤트 기반 무효화 (동적 데이터)
 * - 비인기 검색어: 캐싱하지 않음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PopularSearchService popularSearchService;
    
    @Value("${app.cache.search-results.duration-minutes:5}")
    private int searchResultCacheMinutes; // 검색 결과 캐시 기간 (기본 5분)
    
    @Value("${app.cache.popularity-threshold:3}")
    private int popularityThreshold; // 인기 검색어 판단 임계값
    
    // 캐시 키 프리픽스
    private static final String PROJECT_CACHE_PREFIX = "search:project:result:";
    private static final String USER_CACHE_PREFIX = "search:user:result:";
    private static final String PROJECT_COUNT_PREFIX = "count:project:";
    private static final String USER_COUNT_PREFIX = "count:user:";
    
    /**
     * 프로젝트 검색이 인기 검색인지 판단하고 캐싱 여부 결정
     */
    public boolean shouldCacheProjectSearch(ProjectSearchReq request) {
        return isPopularProjectSearch(request);
    }
    
    /**
     * 사용자 검색이 인기 검색인지 판단하고 캐싱 여부 결정
     */
    public boolean shouldCacheUserSearch(UserSearchReq request) {
        return isPopularUserSearch(request);
    }
    
    /**
     * 인기 검색어 기반으로 프로젝트 검색의 인기도 판단
     */
    private boolean isPopularProjectSearch(ProjectSearchReq request) {
        // 키워드가 인기 검색어 목록에 있는지 확인
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            List<String> popularKeywords = popularSearchService.getPopularKeywords();
            String keyword = request.getKeyword().trim().toLowerCase();
            
            boolean isPopularKeyword = popularKeywords.stream()
                    .anyMatch(popular -> popular.toLowerCase().equals(keyword));
            
            if (isPopularKeyword) {
                log.debug("Popular keyword detected: {}", keyword);
                return true;
            }
        }
        
        // 기술스택이 인기 기술스택 목록에 있는지 확인
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            List<String> popularTechStacks = popularSearchService.getPopularTechStacks();
            
            boolean hasPopularTechStack = request.getTechStacks().stream()
                    .anyMatch(popularTechStacks::contains);
            
            if (hasPopularTechStack) {
                log.debug("Popular tech stack detected: {}", request.getTechStacks());
                return true;
            }
        }
        
        // 기술파트가 인기 기술파트 목록에 있는지 확인
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            List<String> popularTechParts = popularSearchService.getPopularTechParts();
            
            boolean hasPopularTechPart = request.getTechParts().stream()
                    .anyMatch(popularTechParts::contains);
            
            if (hasPopularTechPart) {
                log.debug("Popular tech part detected: {}", request.getTechParts());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 인기 검색어 기반으로 사용자 검색의 인기도 판단
     */
    private boolean isPopularUserSearch(UserSearchReq request) {
        // 키워드가 인기 사용자 검색어 목록에 있는지 확인
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            List<String> popularUserKeywords = popularSearchService.getPopularUserKeywords();
            String keyword = request.getKeyword().trim().toLowerCase();
            
            boolean isPopularKeyword = popularUserKeywords.stream()
                    .anyMatch(popular -> popular.toLowerCase().equals(keyword));
            
            if (isPopularKeyword) {
                log.debug("Popular user keyword detected: {}", keyword);
                return true;
            }
        }
        
        // 기술스택이 인기 사용자 기술스택 목록에 있는지 확인
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            List<String> popularUserTechStacks = popularSearchService.getPopularUserTechStacks();
            
            boolean hasPopularTechStack = request.getTechStacks().stream()
                    .anyMatch(popularUserTechStacks::contains);
            
            if (hasPopularTechStack) {
                log.debug("Popular user tech stack detected: {}", request.getTechStacks());
                return true;
            }
        }
        
        // 기술파트가 인기 사용자 기술파트 목록에 있는지 확인
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            List<String> popularUserTechParts = popularSearchService.getPopularUserTechParts();
            
            boolean hasPopularTechPart = request.getTechParts().stream()
                    .anyMatch(popularUserTechParts::contains);
            
            if (hasPopularTechPart) {
                log.debug("Popular user tech part detected: {}", request.getTechParts());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 프로젝트 검색 결과 캐시 키 생성
     */
    public String generateProjectCacheKey(ProjectSearchReq request) {
        StringBuilder keyBuilder = new StringBuilder(PROJECT_CACHE_PREFIX);
        
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            keyBuilder.append("kw:").append(request.getKeyword().trim().toLowerCase()).append(":");
        }
        
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            keyBuilder.append("ts:").append(String.join(",", request.getTechStacks())).append(":");
        }
        
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            keyBuilder.append("tp:").append(String.join(",", request.getTechParts())).append(":");
        }
        
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            keyBuilder.append("st:").append(String.join(",", request.getStatuses())).append(":");
        }
        
        keyBuilder.append("page:").append(request.getPage())
                  .append(":size:").append(request.getSize());
        
        if (request.getSortBy() != null && !request.getSortBy().trim().isEmpty()) {
            keyBuilder.append(":sort:").append(request.getSortBy());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 사용자 검색 결과 캐시 키 생성
     */
    public String generateUserCacheKey(UserSearchReq request) {
        StringBuilder keyBuilder = new StringBuilder(USER_CACHE_PREFIX);
        
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            keyBuilder.append("kw:").append(request.getKeyword().trim().toLowerCase()).append(":");
        }
        
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            keyBuilder.append("ts:").append(String.join(",", request.getTechStacks())).append(":");
        }
        
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            keyBuilder.append("tp:").append(String.join(",", request.getTechParts())).append(":");
        }
        
        if (request.getExperienceRanges() != null && !request.getExperienceRanges().isEmpty()) {
            keyBuilder.append("exp:").append(request.getExperienceRanges().toString()).append(":");
        }
        
        keyBuilder.append("page:").append(request.getPage())
                  .append(":size:").append(request.getSize());
        
        if (request.getSortBy() != null && !request.getSortBy().trim().isEmpty()) {
            keyBuilder.append(":sort:").append(request.getSortBy());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 프로젝트 검색 결과 캐시에서 조회
     */
    public SearchResult<ProjectSearchRes> getCachedProjectSearchResult(ProjectSearchReq request) {
        try {
            String cacheKey = generateProjectCacheKey(request);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                SearchResult<ProjectSearchRes> result = objectMapper.readValue(
                    cached.toString(), 
                    new TypeReference<SearchResult<ProjectSearchRes>>() {}
                );
                log.info("Cache HIT for project search: {}", cacheKey);
                return result;
            }
            
            log.debug("Cache MISS for project search: {}", cacheKey);
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to get cached project search result: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 사용자 검색 결과 캐시에서 조회
     */
    public SearchResult<UserSearchRes> getCachedUserSearchResult(UserSearchReq request) {
        try {
            String cacheKey = generateUserCacheKey(request);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                SearchResult<UserSearchRes> result = objectMapper.readValue(
                    cached.toString(), 
                    new TypeReference<SearchResult<UserSearchRes>>() {}
                );
                log.info("Cache HIT for user search: {}", cacheKey);
                return result;
            }
            
            log.debug("Cache MISS for user search: {}", cacheKey);
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to get cached user search result: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 프로젝트 검색 결과를 캐시에 저장 (짧은 TTL)
     */
    public void cacheProjectSearchResult(ProjectSearchReq request, SearchResult<ProjectSearchRes> result) {
        try {
            String cacheKey = generateProjectCacheKey(request);
            Duration cacheDuration = Duration.ofMinutes(searchResultCacheMinutes);
            
            String jsonResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonResult, cacheDuration);
            
            log.info("Cached project search result: {} (TTL: {} minutes)", cacheKey, searchResultCacheMinutes);
            
        } catch (Exception e) {
            log.error("Failed to cache project search result: {}", e.getMessage());
        }
    }
    
    /**
     * 사용자 검색 결과를 캐시에 저장 (짧은 TTL)
     */
    public void cacheUserSearchResult(UserSearchReq request, SearchResult<UserSearchRes> result) {
        try {
            String cacheKey = generateUserCacheKey(request);
            Duration cacheDuration = Duration.ofMinutes(searchResultCacheMinutes);
            
            String jsonResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonResult, cacheDuration);
            
            log.info("Cached user search result: {} (TTL: {} minutes)", cacheKey, searchResultCacheMinutes);
            
        } catch (Exception e) {
            log.error("Failed to cache user search result: {}", e.getMessage());
        }
    }
    
    /**
     * 프로젝트 검색 결과 캐시 무효화
     */
    public void evictProjectSearchCache() {
        try {
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PROJECT_CACHE_PREFIX + "*")));
            log.info("Evicted all project search result cache");
        } catch (Exception e) {
            log.error("Failed to evict project search cache: {}", e.getMessage());
        }
    }
    
    /**
     * 사용자 검색 결과 캐시 무효화
     */
    public void evictUserSearchCache() {
        try {
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(USER_CACHE_PREFIX + "*")));
            log.info("Evicted all user search result cache");
        } catch (Exception e) {
            log.error("Failed to evict user search cache: {}", e.getMessage());
        }
    }
    
    /**
     * 모든 검색 결과 캐시 무효화 (인기 검색어/기술스택은 보존)
     */
    public void evictAllSearchResultCache() {
        evictProjectSearchCache();
        evictUserSearchCache();
        log.info("Evicted all search result caches (popular data preserved)");
    }
}