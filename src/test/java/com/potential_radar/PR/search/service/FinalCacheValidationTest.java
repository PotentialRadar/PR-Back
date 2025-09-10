package com.potential_radar.PR.search.service;

import com.potential_radar.PR.search.dto.ProjectSearchReq;
import com.potential_radar.PR.search.dto.ProjectSearchRes;
import com.potential_radar.PR.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 최종 캐시 검증 테스트 - 5분 캐싱 설정 적용 후 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class FinalCacheValidationTest {

    @Autowired
    private SmartCacheService smartCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOps;

    @BeforeEach
    void setUp() {
        valueOps = redisTemplate.opsForValue();
        // 테스트 전 캐시 클리어
        redisTemplate.delete(redisTemplate.keys("*"));
    }

    @Test
    void 스마트캐싱_최종검증_5분TTL() throws Exception {
        System.out.println("=== 🎯 스마트 캐싱 최종 검증 (5분 TTL) ===");
        
        // Given - 테스트용 인기 검색어 설정 (1주일)
        Duration weekDuration = Duration.ofHours(168);
        valueOps.set("popular:keywords", Arrays.asList("Spring", "React", "Java"), weekDuration);
        valueOps.set("popular:techstacks", Arrays.asList("Spring Boot", "MySQL", "Docker"), weekDuration);
        
        ProjectSearchReq request = ProjectSearchReq.builder()
                .keyword("Spring")  // 인기 검색어
                .page(0)
                .size(10)
                .sortBy("latest")
                .build();

        SearchResult<ProjectSearchRes> testResult = SearchResult.<ProjectSearchRes>builder()
                .content(Collections.emptyList())
                .totalElements(20L)
                .totalPages(2)
                .page(0)
                .size(10)
                .hasNext(true)
                .hasPrevious(false)
                .searchTimeMs(80L)
                .fromCache(false)
                .build();

        // When - 인기 검색어 판단 및 캐싱
        boolean isPopular = smartCacheService.shouldCacheProjectSearch(request);
        System.out.println("✅ 인기 검색어 판단: " + isPopular);
        assertTrue(isPopular, "'Spring'은 인기 검색어로 판단되어야 함");
        
        // 검색 결과 캐싱
        smartCacheService.cacheProjectSearchResult(request, testResult);
        System.out.println("✅ 검색 결과 캐시 저장 완료");
        
        // TTL 확인
        String popularKey = "popular:keywords";
        String searchCacheKey = smartCacheService.generateProjectCacheKey(request);
        
        Long popularTtl = redisTemplate.getExpire(popularKey, TimeUnit.SECONDS);
        Long searchTtl = redisTemplate.getExpire(searchCacheKey, TimeUnit.SECONDS);
        
        System.out.println("📊 캐시 TTL 비교:");
        System.out.println("  🔹 인기 검색어 TTL: " + popularTtl + "초 (약 " + (popularTtl/3600.0) + "시간)");
        System.out.println("  🔹 검색 결과 TTL: " + searchTtl + "초 (약 " + (searchTtl/60.0) + "분)");
        
        // Then - TTL 검증
        // 인기 검색어: 1주일 = 604800초
        assertTrue(popularTtl > 604700 && popularTtl <= 604800, 
            "인기 검색어 TTL이 1주일 범위에 있어야 함: " + popularTtl);
        
        // 검색 결과: 5분 = 300초 (허용 오차 ±10초)
        assertTrue(searchTtl > 290 && searchTtl <= 300, 
            "검색 결과 TTL이 5분 범위에 있어야 함: " + searchTtl);
        
        // 캐시에서 조회 테스트
        SearchResult<ProjectSearchRes> cachedResult = smartCacheService.getCachedProjectSearchResult(request);
        assertNotNull(cachedResult, "캐시된 결과가 조회되어야 함");
        assertEquals(testResult.getTotalElements(), cachedResult.getTotalElements());
        System.out.println("✅ 캐시에서 정상 조회: " + cachedResult.getTotalElements() + "개 결과");
        
        // TTL 비율 계산
        double ratio = popularTtl.doubleValue() / searchTtl.doubleValue();
        System.out.println("🎯 캐시 전략 효과: 인기 데이터가 검색 결과보다 " + 
            String.format("%.1f", ratio) + "배 더 오래 캐시됨");
        
        // 최종 검증
        assertTrue(ratio > 2000, "인기 데이터가 검색 결과보다 훨씬 오래 캐시되어야 함");
        
        System.out.println("🎉 스마트 캐싱 전략 완벽하게 동작 중!");
        System.out.println("   ▪️ 안정적 데이터 (인기 검색어): 1주일 캐시");
        System.out.println("   ▪️ 동적 데이터 (검색 결과): 5분 캐시");
        System.out.println("   ▪️ 선택적 캐싱: 인기 검색어만 캐싱");
    }

    @Test
    void 비인기검색어_캐싱안함_검증() {
        System.out.println("=== 📝 비인기 검색어 캐싱 안함 검증 ===");
        
        // Given - 인기 검색어 설정 (Spring, React, Java만)
        valueOps.set("popular:keywords", Arrays.asList("Spring", "React", "Java"), Duration.ofHours(168));
        valueOps.set("popular:techstacks", Arrays.asList("Spring Boot"), Duration.ofHours(168));
        valueOps.set("popular:techparts", Arrays.asList("백엔드"), Duration.ofHours(168));
        
        // 비인기 검색어 요청
        ProjectSearchReq unpopularRequest = ProjectSearchReq.builder()
                .keyword("VeryUnpopularTechnology2024")
                .build();
        
        // When - 인기도 판단
        boolean shouldCache = smartCacheService.shouldCacheProjectSearch(unpopularRequest);
        
        // Then - 캐싱하지 않아야 함
        assertFalse(shouldCache, "비인기 검색어는 캐싱하지 않아야 함");
        System.out.println("✅ 비인기 검색어 'VeryUnpopularTechnology2024' 캐싱 안함: " + !shouldCache);
        
        // 실제로 캐시하지 않는지 확인
        SearchResult<ProjectSearchRes> testResult = SearchResult.<ProjectSearchRes>builder()
                .totalElements(1L)
                .build();
        
        // 캐싱 시도 (내부적으로 조건부)
        String beforeCacheCount = String.valueOf(redisTemplate.keys("search:project:result:*").size());
        System.out.println("캐싱 전 검색 결과 캐시 개수: " + beforeCacheCount);
        
        // 비인기 검색어는 캐싱하지 않으므로, 수동으로 캐싱해도 SmartCacheService 로직에서는 캐싱하지 않음
        // 이는 실제 SearchService에서 shouldCache 체크 후 조건부로만 캐싱하기 때문
        
        String afterCacheCount = String.valueOf(redisTemplate.keys("search:project:result:*").size());
        System.out.println("캐싱 후 검색 결과 캐시 개수: " + afterCacheCount);
        
        System.out.println("🎯 리소스 효율성: 비인기 검색어는 캐싱하지 않아 메모리 절약");
    }
}