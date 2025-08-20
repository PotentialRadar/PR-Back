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
    
    private static final String SEARCH_CACHE_PREFIX = "search:project:";
    
    public String generateCacheKey(ProjectSearchReq request) {
        // 검색 조건을 기반으로 고유한 캐시 키 생성
        StringBuilder keyBuilder = new StringBuilder(SEARCH_CACHE_PREFIX);
        
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
        
        return keyBuilder.toString();
    }
    
    public SearchResult<ProjectSearchRes> getCachedSearchResult(ProjectSearchReq request) {
        try {
            String cacheKey = generateCacheKey(request);
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
    
    public void cacheSearchResult(ProjectSearchReq request, SearchResult<ProjectSearchRes> result) {
        try {
            String cacheKey = generateCacheKey(request);
            Duration cacheDuration = Duration.ofMinutes(cacheMinutes);
            
            // SearchResult를 JSON 문자열로 직렬화하여 저장
            String jsonResult = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, jsonResult, cacheDuration);
            
            log.info("Cached search result for key: {} (duration: {} minutes)", cacheKey, cacheMinutes);
            
        } catch (Exception e) {
            log.error("Failed to cache search result: {}", e.getMessage());
        }
    }
    
    public void evictSearchCache(String pattern) {
        try {
            // 패턴 매칭으로 캐시 삭제 (관리자용)
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(SEARCH_CACHE_PREFIX + pattern + "*")));
            log.info("Evicted cache for pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Failed to evict cache: {}", e.getMessage());
        }
    }
    
    public void clearAllSearchCache() {
        try {
            redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(SEARCH_CACHE_PREFIX + "*")));
            log.info("Cleared all search cache");
        } catch (Exception e) {
            log.error("Failed to clear search cache: {}", e.getMessage());
        }
    }
}