package com.potential_radar.PR.search.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 캐시 디버그 테스트 - 현재 캐시 상태 확인
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheDebugTest {

    @Autowired
    private PopularSearchService popularSearchService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void 현재_캐시상태_디버깅() {
        System.out.println("=== 현재 Redis 캐시 상태 확인 ===");
        
        // 1. 모든 Redis 키 확인
        Set<String> allKeys = redisTemplate.keys("*");
        System.out.println("Redis 전체 키 개수: " + allKeys.size());
        allKeys.forEach(key -> {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            System.out.println("Key: " + key + " | TTL: " + ttl + "초");
        });
        
        // 2. 인기 검색어 관련 키 확인
        System.out.println("\n=== 인기 검색어 관련 키 ===");
        Set<String> popularKeys = redisTemplate.keys("popular:*");
        popularKeys.forEach(key -> {
            Object value = redisTemplate.opsForValue().get(key);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            System.out.println("Key: " + key + " | Value: " + value + " | TTL: " + ttl + "초");
        });
        
        // 3. 검색 결과 캐시 키 확인  
        System.out.println("\n=== 검색 결과 캐시 키 ===");
        Set<String> searchKeys = redisTemplate.keys("search:*");
        searchKeys.forEach(key -> {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            System.out.println("Key: " + key + " | TTL: " + ttl + "초");
        });
        
        // 4. PopularSearchService 동작 확인
        System.out.println("\n=== PopularSearchService 상태 ===");
        try {
            List<String> keywords = popularSearchService.getPopularKeywords();
            List<String> techStacks = popularSearchService.getPopularTechStacks();
            List<String> techParts = popularSearchService.getPopularTechParts();
            
            System.out.println("인기 검색어: " + keywords);
            System.out.println("인기 기술스택: " + techStacks);
            System.out.println("인기 기술파트: " + techParts);
            
            // 수동으로 업데이트 실행
            System.out.println("\n인기 데이터 업데이트 실행...");
            popularSearchService.updatePopularItems();
            
            // 업데이트 후 다시 확인
            keywords = popularSearchService.getPopularKeywords();
            techStacks = popularSearchService.getPopularTechStacks();
            techParts = popularSearchService.getPopularTechParts();
            
            System.out.println("업데이트 후 인기 검색어: " + keywords);
            System.out.println("업데이트 후 인기 기술스택: " + techStacks);
            System.out.println("업데이트 후 인기 기술파트: " + techParts);
            
        } catch (Exception e) {
            System.out.println("PopularSearchService 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 5. 최종 Redis 키 상태
        System.out.println("\n=== 최종 Redis 상태 ===");
        allKeys = redisTemplate.keys("*");
        System.out.println("최종 Redis 키 개수: " + allKeys.size());
        allKeys.forEach(key -> {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            Object value = redisTemplate.opsForValue().get(key);
            System.out.println("Key: " + key + " | TTL: " + ttl + "초 | Value type: " + 
                (value != null ? value.getClass().getSimpleName() : "null"));
        });
    }
}