package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.cache.search-results.duration-minutes:10}")
    private int cacheMinutes;
    
    private static final String PROJECT_CACHE_PREFIX = "search:project:";
    private static final String PORTFOLIO_CACHE_PREFIX = "search:portfolio:";
    private static final String PROJECT_COUNT_PREFIX = "count:project:";
    private static final String PORTFOLIO_COUNT_PREFIX = "count:portfolio:";
    
    private static final int POPULARITY_THRESHOLD = 3;
    
    public String generateProjectCacheKey(ProjectSearchReq request) {
        // 검색 조건을 기반으로 고유한 캐시 키 생성
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
        
        keyBuilder.append("page:").append(request.getPage()).append(":")
                  .append("size:").append(request.getSize());
        
        // 정렬 옵션도 캐시 키에 포함
        if (request.getSortBy() != null && !request.getSortBy().trim().isEmpty()) {
            keyBuilder.append(":sort:").append(request.getSortBy());
        }
        
        return keyBuilder.toString();
    }
    
    public boolean shouldCacheProjectSearch(ProjectSearchReq request) {
        boolean hasPopularFilter = false;
        
        // 키워드 인기도 체크
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keywordKey = PROJECT_COUNT_PREFIX + "kw:" + request.getKeyword().trim().toLowerCase();
            Long keywordCount = redisTemplate.opsForValue().increment(keywordKey, 1);
            if (keywordCount == 1) {
                redisTemplate.expire(keywordKey, Duration.ofDays(1));
            }
            if (keywordCount >= POPULARITY_THRESHOLD) {
                hasPopularFilter = true;
            }
        }
        
        // 기술파트 인기도 체크
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            for (String techPart : request.getTechParts()) {
                String partKey = PROJECT_COUNT_PREFIX + "tp:" + techPart;
                Long partCount = redisTemplate.opsForValue().increment(partKey, 1);
                if (partCount == 1) {
                    redisTemplate.expire(partKey, Duration.ofDays(1));
                }
                if (partCount >= POPULARITY_THRESHOLD) {
                    hasPopularFilter = true;
                }
            }
        }
        
        // 기술스택 인기도 체크
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            for (String techStack : request.getTechStacks()) {
                String stackKey = PROJECT_COUNT_PREFIX + "ts:" + techStack;
                Long stackCount = redisTemplate.opsForValue().increment(stackKey, 1);
                if (stackCount == 1) {
                    redisTemplate.expire(stackKey, Duration.ofDays(1));
                }
                if (stackCount >= POPULARITY_THRESHOLD) {
                    hasPopularFilter = true;
                }
            }
        }
        
        return hasPopularFilter;
    }
    
    private String generateCountKey(String prefix, ProjectSearchReq request) {
        StringBuilder keyBuilder = new StringBuilder(prefix);
        
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            keyBuilder.append("kw:").append(request.getKeyword().trim().toLowerCase()).append(":");
        }
        
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            keyBuilder.append("ts:").append(String.join(",", request.getTechStacks())).append(":");
        }
        
        if (request.getTechParts() != null && !request.getTechParts().isEmpty()) {
            keyBuilder.append("tp:").append(String.join(",", request.getTechParts())).append(":");
        }
        
        return keyBuilder.toString();
    }
    
    public SearchResult<ProjectSearchRes> getCachedProjectSearchResult(ProjectSearchReq request) {
        try {
            String cacheKey = generateProjectCacheKey(request);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                // JSON 문자열을 SearchResult로 역직렬화
                SearchResult<ProjectSearchRes> result = objectMapper.readValue(
                    cached.toString(), 
                    new TypeReference<SearchResult<ProjectSearchRes>>() {}
                );
                log.info("Cache HIT for key: {}", cacheKey);
                return result;
            }
            
            log.debug("Cache MISS for key: {}", cacheKey);
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to get cached search result: {}", e.getMessage());
            return null;
        }
    }
    
    public void cacheProjectSearchResult(ProjectSearchReq request, SearchResult<ProjectSearchRes> result) {
        try {
            String cacheKey = generateProjectCacheKey(request);
            Duration cacheDuration = Duration.ofMinutes(cacheMinutes);
            
            // SearchResult를 JSON 문자열로 직렬화하여 저장
            String jsonResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonResult, cacheDuration);
            
            log.info("Cached search result for key: {} (duration: {} minutes)", cacheKey, cacheMinutes);
            
        } catch (Exception e) {
            log.error("Failed to cache search result: {}", e.getMessage());
        }
    }
    
    public void evictProjectSearchCache(String pattern) {
        try {
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PROJECT_CACHE_PREFIX + pattern + "*")));
            log.info("Evicted project cache for pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Failed to evict project cache: {}", e.getMessage());
        }
    }
    
    public void evictPortfolioSearchCache(String pattern) {
        try {
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PORTFOLIO_CACHE_PREFIX + pattern + "*")));
            log.info("Evicted portfolio cache for pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Failed to evict portfolio cache: {}", e.getMessage());
        }
    }
    
    public void clearAllSearchCache() {
        try {
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PROJECT_CACHE_PREFIX + "*")));
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PORTFOLIO_CACHE_PREFIX + "*")));
            log.info("Cleared all search cache");
        } catch (Exception e) {
            log.error("Failed to clear search cache: {}", e.getMessage());
        }
    }
    
    public void clearAllRedisData() {
        try {
            // 모든 캐시 데이터 삭제
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PROJECT_CACHE_PREFIX + "*")));
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PORTFOLIO_CACHE_PREFIX + "*")));
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PROJECT_COUNT_PREFIX + "*")));
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(PORTFOLIO_COUNT_PREFIX + "*")));
            
            // 인기 검색어 데이터 삭제
            redisTemplate.delete("popular:keywords");
            redisTemplate.delete("popular:techstacks");
            redisTemplate.delete("popular:techparts");
            redisTemplate.delete("popular:user:keywords");
            redisTemplate.delete("popular:user:techstacks");
            redisTemplate.delete("popular:user:techparts");
            
            log.info("Cleared all Redis data completely");
        } catch (Exception e) {
            log.error("Failed to clear all Redis data: {}", e.getMessage());
        }
    }
}